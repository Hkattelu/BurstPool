package com.github.Chronox.pool.actors
import com.github.Chronox.pool.Global
import com.github.Chronox.pool.db.{User, PoolPayment}

import akka.actor.{Actor, ActorLogging}
import akka.util.{Timeout, ByteString}
import scala.util.{Failure, Success}
import scala.collection.concurrent.TrieMap
import scala.concurrent.{Future, Await}
import scala.concurrent.duration._
import language.postfixOps
import java.math.BigInteger

case class addPendingPayment(user: User, nqt: Long)
case class payPendingPayment(user: User, nqt: Long)

class PaymentLedger extends Actor with ActorLogging {

  var payments: TrieMap[Long, PoolPayment] =
    TrieMap[Long, PoolPayment]()
  val burstToNQT = 100000000L

  override def preStart() {
    payments = Global.poolDB.loadPoolPayments()
  }

  def receive() = {
    case addPendingRewards(rewards: List[Reward]) => {
      val blockToNQTMap = Global.poolDB.blocksToRewardNQT(
        rewards.map{case(r) => r.blockId})
      for (reward <- rewards)
        self ! addPendingPayment(reward.userId, blockToNQTMap(reward.blockId))
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
    }
    case payPendingPayment(id: Long, nqt: Long) => {
      var payment = payments(id)
      payment.pendingNQT -= nqt
      payment.paidNQT += nqt
      payments(id) = payment
      Global.dbWriter ! writeFunction(
        () => Global.poolDB.updatePayment(payment))
    }
  }
}