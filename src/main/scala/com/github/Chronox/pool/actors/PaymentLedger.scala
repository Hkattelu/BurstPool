package com.github.Chronox.pool.actors
import com.github.Chronox.pool.{Global, Config}
import com.github.Chronox.pool.db.{User, Reward, PoolPayment}

import akka.actor.{Actor, ActorLogging}
import akka.util.{Timeout, ByteString}
import akka.pattern.ask
import scala.util.{Failure, Success}
import scala.collection.concurrent.TrieMap
import scala.concurrent.{Future, ExecutionContext, Await}
import scala.concurrent.duration._
import language.postfixOps
import java.math.BigInteger

case class addPendingRewards(rewards: List[Reward])
case class addPendingPayment(id: Long, nqt: Long)
case class payPendingPayment(id: Long, nqt: Long)
case class clearPayments()
case class getPaymentsMap()
case class getPayments()
case class getUserPayment(id: Long)

class PaymentLedger extends Actor with ActorLogging {

  import context.dispatcher
  final implicit val timeout: Timeout = 5 seconds
  var payments: TrieMap[Long, PoolPayment] =
    TrieMap[Long, PoolPayment]()

  override def preStart() {
    payments = Global.poolDB.loadPoolPayments()
  }

  def receive() = {
    case addPendingRewards(rewards: List[Reward]) => {
      val blockIds = rewards.map{case(r) => r.blockId}
      val blockToNQTMapFuture = (Global.dbReader ? readFunction(
        () => Global.poolDB.blocksToRewardNQT(blockIds)))
        .mapTo[TrieMap[Long, Long]]
      blockToNQTMapFuture onComplete {
        case Success(blockToNQTMap) => {
          for (reward <- rewards) {
            val rewardPercent = 
              (reward.currentPercent * Config.CURRENT_BLOCK_SHARE) +
              (reward.historicalPercent * Config.HISTORIC_BLOCK_SHARE)
            var amount: Long = (rewardPercent * BigDecimal.valueOf(
              blockToNQTMap(reward.blockId))).longValue()
            self ! addPendingPayment(reward.userId, amount)
          }
        }
        case Failure(error) => 
          log.error("Get blockToNQTMap error: " + error.toString)
      }
    }
    case addPendingPayment(id: Long, nqt: Long) => {
      payments contains id match {
        case false => {
          var payment = new PoolPayment()
          payment.id = id
          payment.pendingNQT = nqt
          payments += (id->payment)
          Global.dbWriter ! writeFunction(
            () => Global.poolDB.addPayment(payment))
        }
        case true => {
          var payment = payments(id)
          payment.pendingNQT += nqt
          payments(id) = payment
          Global.dbWriter ! writeFunction(
            () => Global.poolDB.updatePayment(payment))
        }
      }
      Global.poolStatistics.incrementNQTEarned(nqt)
    }
    case payPendingPayment(id: Long, nqt: Long) => {
      var payment = payments(id)
      payment.pendingNQT -= (nqt + Global.burstToNQT) // TX fee
      payment.paidNQT += nqt
      payments(id) = payment
      Global.dbWriter ! writeFunction(
        () => Global.poolDB.updatePayment(payment))
    }
    case clearPayments() => payments.clear()
    case getPaymentsMap() => sender ! payments.toMap
    case getPayments() => sender ! payments.values
    case getUserPayment(id: Long) => {
      payments contains id match {
        case true => sender ! payments(id) 
        case false => sender ! payments.values
      }
    }
  }
}