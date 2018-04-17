package com.github.Chronox.pool.actors
import com.github.Chronox.pool.{Global, Config}
import com.github.Chronox.pool.db.{Share, Reward}

import akka.actor.{Actor, ActorLogging}
import akka.http.scaladsl.Http
import akka.http.scaladsl.model._
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

case class addRewards(blockId: Long, 
  currentSharePercents: Map[Long, BigDecimal],
  historicSharePercents: Map[Long, BigDecimal])
case class BlockResponse(blockReward: String, totalFeeNQT: String, 
  block: String)
case class TransactionResponse(transaction: String, broadcast: Boolean)
case class getRewards()
case class PayoutRewards()
case class clearRewards()

class RewardPayout extends Actor with ActorLogging {

  import context.dispatcher 

  implicit val formats = DefaultFormats
  final implicit val materializer: ActorMaterializer = 
    ActorMaterializer(ActorMaterializerSettings(context.system))
  final implicit val timeout: Timeout = 4 seconds

  var unpaidRewards: TrieMap[Long, List[Reward]] = TrieMap[Long, List[Reward]]()
  val burstToNQT = 100000000L
  val http = Http(context.system)
  var baseURI = Uri(Config.NODE_ADDRESS + "/burst")

  override def preStart() {
    //unpaidRewards = Global.poolDB.loadRewardShares()
  }

  def receive() = {
    case PayoutRewards() => {
      var blockToNQT = scala.collection.mutable.Map[Long, Long]()
      var userToRewards = 
        scala.collection.mutable.Map[Long, ListBuffer[Reward]]()
      var responseFutureList = new ListBuffer[Future[HttpResponse]]()

      // Send out requests for block reward information
      for((blockId, rewards) <- unpaidRewards) 
        responseFutureList += http.singleRequest(HttpRequest(
          uri = baseURI+"?requestType=getBlock&block="+blockId.toString))
      // Block until we get the responses, then handle them
      for(future <- responseFutureList) {   
        Await.ready(future, timeout.duration).value.get match {
          case Success(res: HttpResponse) => {
            res.entity.dataBytes.runFold(ByteString(""))(_ ++ _).foreach 
              { body =>
              val blockRes = parse(body.utf8String).extract[BlockResponse]
              val rewardNQT = (blockRes.blockReward.toLong * burstToNQT) +
                blockRes.totalFeeNQT.toLong
              val blockId = blockRes.block.toLong
              blockToNQT += (blockId->((1-Config.POOL_FEE) * rewardNQT).toLong)
              // Note the rewards that each user should be getting paid
              for(r <- unpaidRewards.getOrElse(blockId, List[Reward]())) {
                if (userToRewards contains r.userId) 
                  userToRewards(r.userId).append(r)
                else 
                  userToRewards += (r.userId->ListBuffer[Reward](r))
              }
            }
          }
          case Failure(e) => log.error(
            "Failed to receive block information:" + e.toString())
        }
      }
      Thread.sleep(100)
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

        // Ask to create a transaction if the reward was more than the tx fee
        if (amount > 0) {
          val ent = FormData(Map("requestType"->"sendMoney", "deadline"->"1440",
            "feeNQT"->burstToNQT.toString, "secretPhrase"->Config.SECRET_PHRASE,
            "recipient"->id.toString, "amountNQT"->amount.toString)).toEntity
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
                    for(reward <- sentRewards) reward.isPaid = true
                    unpaidRewards = unpaidRewards.map{ 
                      case(k,v) => (k, v.filter(!_.isPaid))}
                    unpaidRewards.retain((k,v) => !v.isEmpty)      
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
    case addRewards(blockId: Long, 
      currentSharePercents: Map[Long, BigDecimal],
      historicSharePercents: Map[Long, BigDecimal]) => {
      // Add historical rewards for the block
      var rewards = historicSharePercents.map{case(k,v) => 
        (k, new Reward(k, blockId, 0.0, v, false))}

      // Add current share rewards for the block, if they exist
      for((id, percent) <- currentSharePercents) rewards contains id match {
        case true => rewards(id).currentPercent = percent
        case false => rewards += (
          id->(new Reward(id, blockId, percent, 0.0, false)))
      } 
      unpaidRewards += (blockId->rewards.values.toList)
    }
    case getRewards() => sender ! unpaidRewards.toMap
    case clearRewards() => unpaidRewards = TrieMap[Long, List[Reward]]()
    case Global.setSubmitURI(uri: String) => baseURI = Uri(uri)   
  }
}