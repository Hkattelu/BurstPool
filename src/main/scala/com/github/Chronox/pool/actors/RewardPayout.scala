package com.github.Chronox.pool.actors

import com.github.Chronox.pool.Config
import com.github.Chronox.pool.db.{Share, Reward}

import akka.actor.{ Actor, ActorLogging }
import akka.http.scaladsl.Http
import akka.http.scaladsl.model._
import akka.stream.{ ActorMaterializer, ActorMaterializerSettings }
import scala.util.{ Failure, Success }
import HttpMethods._
import akka.util.Timeout
import scala.collection.concurrent.TrieMap
import scala.collection.JavaConversions._
import scala.concurrent.duration._
import scala.util.{Failure, Success}
import net.liftweb.json._

import java.lang.Long
import java.math.BigInteger
import java.util.concurrent.ConcurrentLinkedQueue

case class addRewards(blockId: BigInteger, 
  currentSharePercents: Map[Long, BigDecimal],
  historicSharePercents: Map[Long, BigDecimal])
case class BlockResponse(totalAmountNQT: String, totalFeeNQT: String)
case class TransactionResponse(transaction: String, broadcast: Boolean)
case class PayoutRewards()

class RewardPayout extends Actor with ActorLogging {

  import context.dispatcher 

  implicit val formats = DefaultFormats
  final implicit val materializer: ActorMaterializer = 
    ActorMaterializer(ActorMaterializerSettings(context.system))

  var rewardsToPay = TrieMap[BigInteger, List[Reward]]()
  val burstToNQT = 100000000L
  val http = Http(context.system)
  val baseTxURI = (Config.NODE_ADDRESS + 
    "/burst?requestType=sendMoney&deadline=1440&feeNQT="+burstToNQT.toString+
    "&secretPhrase="+Config.SECRET_PHRASE)
  val baseBlockURI = Config.NODE_ADDRESS + "/burst?requestType=getBlock&block="

  def receive() = {
    case PayoutRewards() => {
      var blockToNQT = scala.collection.mutable.Map[BigInteger, Long]()
      var userToRewards = scala.collection.mutable.Map[Long, List[Reward]]()
      for((blockId, rewards) <- rewardsToPay) {
        http.singleRequest(
          HttpRequest(uri = baseBlockURI + blockId.toString())
        ) onComplete {
          case Success(res: HttpResponse) => {
            val blockRes = parse(res.entity.toString()).extract[BlockResponse]
            val rewardNQT = (Long.parseUnsignedLong(blockRes.totalAmountNQT) +
              Long.parseUnsignedLong(blockRes.totalFeeNQT))
            blockToNQT += (blockId->((1-Config.POOL_FEE) * rewardNQT).toLong)
          }
          case Failure(error) => {
            log.error("Failed to get block info: " + error.toString())
          }
        }
        for(reward <- rewards) {
          if (userToRewards contains reward.userId) 
            reward :: userToRewards(reward.userId)
          else 
            userToRewards += (reward.userId->List[Reward](reward))
        } 
      }

      for((id, rewards) <- userToRewards) {
        var amount: Long = 0 - burstToNQT
        var markAsPaid = List[Reward]()
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
            case Failure(error) => log.error(error.toString())
          }
        } else {
          log.info("User " + id + " could not pay the TX fee")
        }
      }
    }
    case addRewards(blockId: BigInteger, 
      currentSharePercents: Map[Long, BigDecimal],
      historicSharePercents: Map[Long, BigDecimal]) => {
      var rewards = historicSharePercents.map{case(k,v) => 
        (k, new Reward(k, blockId, 0.0, v, false))}
      for((id, percent) <- currentSharePercents){
        if (rewards contains id) rewards(id).currentPercent = percent
        else rewards += (id->(new Reward(id, blockId, percent, 0.0, false)))
      }
      rewardsToPay += (blockId->rewards.values.toList)
    }
  }
}