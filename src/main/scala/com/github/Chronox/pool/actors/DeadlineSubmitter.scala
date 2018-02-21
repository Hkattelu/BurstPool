package com.github.Chronox.pool.actors

import com.github.Chronox.pool.Global

import akka.actor.{ Actor, ActorLogging }
import akka.util.Timeout
import org.scalatra._
import scala.concurrent.duration._
import org.json4s.{DefaultFormats, Formats}
import org.scalatra.json._ 

case class verifyNonce(accountId: String, nonce: String)
case class isBestNonce(ip_address: String, accountId: String, nonce: String)
case class submitNonce(accountId: String, nonce: String)

class DeadlineSubmitter extends Actor with ActorLogging {

  protected implicit lazy val jsonFormats: Formats = 
    DefaultFormats.withBigDecimal
  protected implicit val timeout: Timeout = 5 seconds

  def receive() = {
    case verifyNonce(accountId: String, nonce: String) => {}
    case isBestNonce(ip_address: String, accountId: String, nonce: String) => {}
    case submitNonce(accountId: String, nonce: String) => {}
  }

  def calculateDeadline(accountId: String, nonce: String): Long  = {
    return 0L
  }

  def checkValidDeadline(deadline: Long): Boolean = {
    return false
  }
}