package com.github.Chronox.pool.actors
import com.github.Chronox.pool.{Global, Config}
import com.github.Chronox.pool.db.{Share, Reward}

import akka.actor.{Actor, ActorLogging}
import akka.http.scaladsl.Http
import akka.http.scaladsl.model._
import akka.pattern.ask
import akka.stream.{ActorMaterializer, ActorMaterializerSettings}
import akka.util.{Timeout, ByteString}
import scala.util.{Failure, Success}
import scala.collection.concurrent.TrieMap
import scala.collection.JavaConversions._
import scala.concurrent.{Future, Await}
import scala.concurrent.duration._
import scala.collection.mutable.ListBuffer
import net.liftweb.json._
import HttpMethods._
import language.postfixOps
import java.math.BigInteger

case class BlockResponse(blockReward: String, totalFeeNQT: String, 
  block: String)
case class TransactionResponse(transaction: String, broadcast: Boolean)
case class payoutRewards()

class RewardPayout extends Actor with ActorLogging {

  import context.dispatcher 

  implicit val formats = DefaultFormats
  final implicit val materializer: ActorMaterializer = 
    ActorMaterializer(ActorMaterializerSettings(context.system))
  final implicit val timeout: Timeout = 10 seconds

  val http = Http(context.system)
  var baseURI = Uri(Config.NODE_ADDRESS + "/burst")

  def receive() = {
    case payoutRewards() => {
      var unpaidRewards: Map[Long, List[Reward]] = Map[Long, List[Reward]]()
      var blockToNQT: TrieMap[Long, Long] = TrieMap[Long, Long]()

      val unpaidRewardFuture = (Global.rewardAccumulator ? getUnpaidRewards())
        .mapTo[Map[Long, List[Reward]]]
      unpaidRewardFuture andThen {
        case Success(unpaidRewardsMap) => {
          unpaidRewards = unpaidRewardsMap
          var blockToNQTFuture = (Global.dbReader ? readFunction(
            () => Global.poolDB.blocksToRewardNQT(unpaidRewards.keys.toList)))
            .mapTo[TrieMap[Long, Long]]
          blockToNQTFuture onComplete {
            case Success(blockToNQTMap) => {
              blockToNQT = blockToNQTMap
              var userToRewards = 
                scala.collection.mutable.Map[Long, ListBuffer[Reward]]()

              // Note the rewards that each user should be getting paid
              for((blockId, rewards) <- unpaidRewards)
                for(r <- unpaidRewards.getOrElse(blockId, List[Reward]())) {
                  if (userToRewards contains r.userId) 
                    userToRewards(r.userId).append(r)
                  else 
                    userToRewards += (r.userId->ListBuffer[Reward](r))
                }

              var userToNQTAmount =
                scala.collection.mutable.Map[Long, Long]()
              // Compute total reward for each user, then pay it out
              for((id, rewards) <- userToRewards) {
                var amount: Long = 0 - Global.burstToNQT
                var sentRewards = new ListBuffer[Reward]()

                // Calculate the actual reward values in NQTs
                for(reward <- rewards.toList) {
                  val rewardPercent =
                    (reward.currentPercent * Config.CURRENT_BLOCK_SHARE) +
                    (reward.historicalPercent * Config.HISTORIC_BLOCK_SHARE)
                  if(blockToNQT contains reward.blockId){
                    amount += (rewardPercent * BigDecimal.valueOf(
                      blockToNQT(reward.blockId))).longValue()
                    sentRewards += reward
                  }
                }

                // Create a transaction if the reward was more than the tx fee
                if (amount > 0) {
                  userToNQTAmount += (id->amount)
                  val ent = FormData(Map("requestType"->"sendMoney", 
                    "deadline"->"1440", "feeNQT"->Global.burstToNQT.toString, 
                    "secretPhrase"->Config.SECRET_PHRASE, 
                    "recipient"->Global.toUnsignedLong(id).toString, 
                    "amountNQT"->amount.toString)).toEntity
                  http.singleRequest(HttpRequest(method = POST, uri = baseURI, 
                    entity = ent)
                  ) onComplete {
                    case Success(res: HttpResponse) => {
                      res.entity.dataBytes.runFold(ByteString(""))(_ ++ _)
                      .foreach { body =>
                        val data = body.utf8String
                        if (data contains "transaction"){
                          val tx = 
                            parse(body.utf8String).extract[TransactionResponse]
                          if(tx.broadcast){
                            log.info("Tx " + tx.transaction + " broadcasted!")
                            val rewardsList = sentRewards.toList
                            for(reward <- rewardsList)reward.isPaid = true
                            Global.paymentLedger ! payPendingPayment(id, 
                              userToNQTAmount(id))
                            Global.dbWriter ! writeFunction(
                              () => Global.poolDB.markRewardsAsPaid(
                                rewardsList))
                            Global.rewardAccumulator ! dumpPaidRewards(
                              rewardsList)
                          } else {
                            log.error(
                              "Tx " + tx.transaction + " was not broadcasted")
                          }
                        } else {
                          val e = parse(data).extract[Global.ErrorMessage]
                          log.error("Error code: " + e.errorCode + ", " + 
                            e.errorDescription)
                        }
                      }
                    }
                    case Failure(error) => log.error(
                      "Transaction failed: " + error.toString())
                  }
                } 
                else log.info(
                  "User " + id.toString + " could not pay the TX fee")
              }
            }
            case Failure(error) =>
              log.error("Failed to get block to NQT map: " + error.toString)
          }
        }
        case Failure(error) =>
          log.error("Failed to load unpaid rewards: " + error.toString)
      }
    }
    case Global.setSubmitURI(uri: String) => baseURI = Uri(uri)   
  }
}