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
    case addPendingPayment(user: User, nqt: Long) => {
      payments contains user.id match {
        case false => {
          var payment = new PoolPayment()
          payment.id = user.id
          payment.nickName = user.nickName
          payment.pendingNQT = nqt
          payments += (user.id->payment)
        }
        case true => {
          var payment = payments(user.id)
          payment.pendingNQT += nqt
          payments(user.id) = payment
        }
      }
    }
    case payPendingPayment(user: User, nqt: Long) => {
      var payment = payments(user.id)
      payment.pendingNQT -= nqt
      payment.paidNQT += nqt
      payments(user.id) = payment
    }
  }
}