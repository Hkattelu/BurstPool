package com.github.Chronox.pool.actors
import com.github.Chronox.pool.{Global, Config}
import com.github.Chronox.pool.db.{Share, Reward}

import akka.actor.{ Actor, ActorLogging }
import akka.http.scaladsl.Http
import akka.http.scaladsl.model._
import akka.stream.{ ActorMaterializer, ActorMaterializerSettings }
import akka.util.Timeout
import scala.util.{ Failure, Success }
import scala.collection.concurrent.TrieMap
import scala.collection.JavaConversions._
import scala.concurrent.{Future, Await}
import scala.concurrent.duration._
import net.liftweb.json._
import HttpMethods._
import language.postfixOps
import java.math.BigInteger

case class addRewards(blockId: Long, 
  currentSharePercents: Map[Long, BigDecimal],
  historicSharePercents: Map[Long, BigDecimal])
case class getRewards()
case class BlockResponse(totalAmountNQT: String, 
  totalFeeNQT: String, block: String)
case class TransactionResponse(transaction: String, broadcast: Boolean)
case class PayoutRewards()

class RewardPayout extends Actor with ActorLogging {

  import context.dispatcher 

  implicit val formats = DefaultFormats
  final implicit val materializer: ActorMaterializer = 
    ActorMaterializer(ActorMaterializerSettings(context.system))
  final implicit val timeout: Timeout = 4 seconds

  var unpaidRewards = TrieMap[Long, List[Reward]]()
  val burstToNQT = 100000000L
  val http = Http(context.system)
  val baseTxURI = (Config.NODE_ADDRESS + 
    "/burst?requestType=sendMoney&deadline=1440&feeNQT="+burstToNQT.toString+
    "&secretPhrase="+Config.SECRET_PHRASE)
  val baseBlockURI = Config.NODE_ADDRESS + "/burst?requestType=getBlock&block="

  def receive() = {
    case PayoutRewards() => {
      var blockToNQT = scala.collection.mutable.Map[Long, Long]()
      var userToRewards = scala.collection.mutable.Map[Long, List[Reward]]()
      var responseFutureList = List[Future[HttpResponse]]()

      // Send out requests for block reward information
      for((blockId, rewards) <- unpaidRewards) 
        http.singleRequest(HttpRequest(uri = baseBlockURI+blockId.toString()))
          .mapTo[HttpResponse] :: responseFutureList

      // Block until we get the responses, then handle them
      for(future <- responseFutureList) {   
        Await.ready(future, timeout.duration).value.get match {
          case Success(res: HttpResponse) => {
            val blockRes = parse(res.entity.toString()).extract[BlockResponse]
            val rewardNQT = blockRes.totalAmountNQT.toLong +
              blockRes.totalFeeNQT.toLong
            val blockId = blockRes.block.toLong

            // Paid out blockNQT is the block value subtracted from pool fee
            blockToNQT += (blockId->((1-Config.POOL_FEE) * rewardNQT).toLong)

            // Note the rewards that each user should be getting paid
            for(reward <- unpaidRewards.getOrElse(blockId, List[Reward]())) {
              if (userToRewards contains reward.userId) 
                reward :: userToRewards(reward.userId)
              else 
                userToRewards += (reward.userId->List[Reward](reward))
            } 
          }
          case Failure(e) => log.error(
            "Failed to receive block information:" + e.toString())
        }
      }

      // Compute total reward for each user, then pay it out
      for((id, rewards) <- userToRewards) {
        var amount: Long = 0 - burstToNQT
        var markAsPaid = List[Reward]()

        // Calculate the actual reward values in NQTs
        for(reward <- rewards) {
          val rewardPercent =
            (reward.currentPercent * Config.CURRENT_BLOCK_SHARE) +
            (reward.historicalPercent * Config.HISTORIC_BLOCK_SHARE)
          if(blockToNQT contains reward.blockId){
            amount += (rewardPercent * BigDecimal.valueOf(
              blockToNQT(reward.blockId))).longValue()
            reward :: markAsPaid
          }
        }

        // Ask to create a transaction if the reward was more than the tx fee
        if (amount > 0) {
          http.singleRequest(HttpRequest(method = POST, 
              uri = baseTxURI+"&recipient="+id+"&amountNQT="+amount)
          ) onComplete {
            case Success(res: HttpResponse) => {
              val txRes = parse(
                res.entity.toString()).extract[TransactionResponse]
              log.info("Tx Id: " + txRes.transaction +
                ", Broadcasted: " + txRes.broadcast)
              for(reward <- markAsPaid) reward.isPaid = true
            }
            case Failure(error) => log.error(
              "Transaction failed: " + error.toString())
          }
        } else {
          log.info("User " + id + " could not pay the TX fee")
        }
      }
    }
    case addRewards(blockId: Long, 
      currentSharePercents: Map[Long, BigDecimal],
      historicSharePercents: Map[Long, BigDecimal]) => {
      // Add historical rewards for the block
      var rewards = historicSharePercents.map{case(k,v) => 
        (k, new Reward(k, blockId, 0.0, v, false))}

      // Add current share rewards for the block, if they exist
      for((id, percent) <- currentSharePercents)
        rewards(id) match {
          case (r: Reward) => r.currentPercent = percent
          case null => rewards += (
            id->(new Reward(id, blockId, percent, 0.0, false)))
        } 
      unpaidRewards += (blockId->rewards.values.toList)
    }
    case getRewards() => sender ! unpaidRewards.toMap
  }
}