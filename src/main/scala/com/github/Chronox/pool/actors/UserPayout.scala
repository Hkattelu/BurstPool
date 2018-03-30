package com.github.Chronox.pool.actors

import com.github.Chronox.pool.Config
import com.github.Chronox.pool.db.Share

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

case class addShares(blockId: BigInteger, shares: List[Share])
case class BlockResponse(totalAmountNQT: String, totalFeeNQT: String)
case class TransactionResponse(transaction: String, broadcast: Boolean)
case class PayoutShares()

class UserPayout extends Actor with ActorLogging {

  import context.dispatcher 

  implicit val formats = DefaultFormats
  final implicit val materializer: ActorMaterializer = 
    ActorMaterializer(ActorMaterializerSettings(context.system))

  var sharesToPay = TrieMap[BigInteger, List[Share]]()
  val burstToNQT = 100000000L
  val http = Http(context.system)
  val baseTxURI = (Config.NODE_ADDRESS + 
    "/burst?requestType=sendMoney&deadline=1440&feeNQT="+burstToNQT.toString+
    "&secretPhrase=" + Config.SECRET_PHRASE)
  val baseBlockURI = Config.NODE_ADDRESS + "/burst?requestType=getBlock&block="

  def weightsToPercents(weights: Map[Long, Long]): Map[Long, Double] = {
    val inverseWeights = 
      weights.map{case (k,v) => (k, 1/v)}.asInstanceOf[Map[Long, Double]]
    var inverseSum = 0.0
    for((k,v) <- inverseWeights) inverseSum += v
    return inverseWeights.map{ case (k,v) => (k, v/inverseSum)}
  }

  def receive() = {
    case PayoutShares() => {
      for((blockId, shares) <- sharesToPay) {
        http.singleRequest(
          HttpRequest(uri = baseBlockURI + blockId.toString())
        ) onComplete {
          case Success(res: HttpResponse) => {
            val blockRes = parse(res.entity.toString()).extract[BlockResponse]
            val rewardNQT = (Long.parseUnsignedLong(blockRes.totalAmountNQT) +
              Long.parseUnsignedLong(blockRes.totalFeeNQT))
            val toPayNQT = ((1-Config.POOL_FEE) * rewardNQT).asInstanceOf[Long]
            var shareWeights = Map[Long, Long]()
            for(share <- shares) shareWeights += (share.userId->share.deadline)
            val sharePercents = weightsToPercents(shareWeights)
            for((id, percent) <- sharePercents) {
              val amount = (toPayNQT * percent).asInstanceOf[Long] - burstToNQT
              if (amount > burstToNQT) {
                http.singleRequest(
                  HttpRequest(method = POST,
                   uri = baseTxURI+"&recipient="+id+"&amountNQT="+amount)
                ) onComplete {
                  case Success(res: HttpResponse) => {
                    val txRes = parse(
                      res.entity.toString()).extract[TransactionResponse]
                    log.info("Tx Id: " + txRes.transaction +
                      ", Broadcasted: " + txRes.broadcast)
                  }
                  case Failure(error) => log.error(error.toString())
                }
              } else {
                log.info("User " + id + " did not have enough burst for fee")
              }
            }
          }
          case Failure(error) => {
            log.error("Failed to get block info: " + error.toString())
          }
        } 
      }
    }
    case addShares(blockId: BigInteger, shares: List[Share]) => {
      sharesToPay += (blockId->shares)
    }
  }

  object historicShareQueue {

    var queue: ConcurrentLinkedQueue[Map[Long, Double]] = 
      new ConcurrentLinkedQueue[Map[Long, Double]]()
    val maxSize = Config.MIN_HEIGHT_DIFF

    def enqueue(map: Map[Long, Double]) {
      queue.add(map)
      if(queue.size() > maxSize) queue.poll()
    }

    def getIterator(): Iterator[Map[Long, Double]] = {
      return queue.iterator()
    }
  }
}