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
case class PayoutRewards()

class RewardPayout extends Actor with ActorLogging {

  import context.dispatcher 

  implicit val formats = DefaultFormats
  final implicit val materializer: ActorMaterializer = 
    ActorMaterializer(ActorMaterializerSettings(context.system))
  final implicit val timeout: Timeout = 10 seconds

  val http = Http(context.system)
  var baseURI = Uri(Config.NODE_ADDRESS + "/burst")

  def receive() = {
    case PayoutRewards() => {
      var unpaidRewards: Map[Long, List[Reward]] = Map[Long, List[Reward]]()
      var blockToNQT = Map[Long, Long] = Map[Long, Long]()

      val rewardFuture = (Global.rewardAccumulator ? getUnpaidRewards())
        .mapTo[Map[Long, List[Reward]]]
      val loadUnpaidRewards = rewardFuture andThen {
        case Success(unpaidRewardsMap: Map[Long, List[Reward]]) =>
          unpaidRewards = unpaidRewardsMap
        case Failure(error) =>
          log.error("Error fetching unpaid rewards: " + error.toString)
      }

      val loadBlockToNQTMap = loadUnpaidRewards andThen {
        var blockToNQTFuture = (Global.dbReader ? readFunction(
          () => Global.poolDB.blockToRewardNQT(unpaidRewards.keys.toList)))
          .mapTo[Map[Long, Long]]
        blockToNQTFuture onComplete {
          case Success(blockToNQTMap: Map[Long, Long]) =>
            blockToNQT = blockToNQTMap
          case Failure(error) =>
            log.error("Error fetching block to NQT map: " + error.toString)
        }
      }

      loadBlockToNQTMap andThen {
        var userToRewards = 
          scala.collection.mutable.Map[Long, ListBuffer[Reward]]()
        var userToNQTAmount =
          scala.collection.mutable.Map[Long, Long]()
        // Compute total reward for each user, then pay it out
        for((id, rewards) <- userToRewards) {
          var amount: Long = 0 - burstToNQT
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
          userToNQTAmount += (reward.userId->amount)

          // Ask to create a transaction if the reward was more than the tx fee
          if (amount > 0) {
            val ent = FormData(Map("requestType"->"sendMoney", 
              "deadline"->"1440", "feeNQT"->burstToNQT.toString, 
              "secretPhrase"->Config.SECRET_PHRASE, "recipient"->id.toString, 
              "amountNQT"->amount.toString)).toEntity
            http.singleRequest(HttpRequest(method = POST, uri = baseURI, 
              entity = ent)
            ) onComplete {
              case Success(res: HttpResponse) => {
                res.entity.dataBytes.runFold(ByteString(""))(_ ++ _).foreach { 
                  body =>
                  val data = body.utf8String
                  if (data contains "transaction"){
                    val tx = parse(body.utf8String).extract[TransactionResponse]
                    if(tx.broadcast){
                      log.info("Tx " + tx.transaction + " broadcasted!")
                      val rewardsList = sentRewards.toList
                      for(reward <- rewardsList) {
                        reward.isPaid = true
                        Global.paymentLedger ! payPendingPayment(reward.userId, 
                          userToNQTAmount(reward.userId))
                      }
                      Global.dbWriter ! writeFunction(
                        () => Global.poolDB.markRewardsAsPaid(rewardsList))
                      Global.rewardAccumulator ! dumpPaidRewards(rewardsList)
                    } else {
                      log.error("Tx " + tx.transaction + " was not broadcasted")
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
          else log.info("User " + id.toString + " could not pay the TX fee")
        }
      }
    }
    case Global.setSubmitURI(uri: String) => baseURI = Uri(uri)   
  }
}