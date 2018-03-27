package com.github.Chronox.pool.actors

import com.github.Chronox.pool.Global
import com.github.Chronox.pool.Config

import akka.actor.{ Actor, ActorLogging }
import akka.http.scaladsl.Http
import akka.http.scaladsl.model._
import akka.stream.{ ActorMaterializer, ActorMaterializerSettings }
import akka.util.ByteString
import scala.concurrent.duration._
import net.liftweb.json._

import java.math.BigInteger

case class ResetBestDeadline()
case class SubmitNonce(accId: Long, nonce: Long, deadline: BigInteger)

class DeadlineSubmitter extends Actor with ActorLogging {

  def isBestDeadline(deadline: BigInteger): Boolean = {
    return deadline.compareTo(Global.currentBestDeadline) <= 0
  }

  def receive() = {
    case ResetBestDeadline() => {
      Global.currentBestDeadline = Config.TARGET_DEADLINE
    }
    case SubmitNonce(accountId: Long, nonce: Long, deadline: BigInteger) => {
      
    }
  }
}