package com.github.Chronox.pool.actors
import com.github.Chronox.pool.{Global, Config}
import com.github.Chronox.pool.db.{Share, Reward}

import akka.actor.{Actor, ActorLogging}
import akka.util.{Timeout, ByteString}
import scala.util.{Failure, Success}
import scala.collection.concurrent.TrieMap
import scala.concurrent.{Future, Await}
import scala.concurrent.duration._
import language.postfixOps
import java.math.BigInteger

case class updatePendingPayments()
case class updateCompletedPayments()

class PaymentLedger extends Actor with ActorLogging {

  var payments: TrieMap[Long, Tuple2[Long, Long]] =
    TrieMap[Long, Tuple2[Long, Long]]()
  val burstToNQT = 100000000L

  override def preStart() {
    payments = Global.poolDB.loadPoolPayments()
  }

  def receive() = {
    case updatePendingPayments() => {}
    case updateCompletedPayments() => {}
  }
}