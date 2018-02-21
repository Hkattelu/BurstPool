package com.github.Chronox.pool.actors

import akka.actor.{ Actor, ActorLogging }
import akka.util.Timeout
import scala.concurrent.duration._
import scala.util.{Failure, Success, Try}
import org.json4s.{DefaultFormats, Formats}

class UserPayout extends Actor with ActorLogging {

  protected implicit lazy val jsonFormats: Formats = 
    DefaultFormats.withBigDecimal
  protected implicit val timeout: Timeout = 5 seconds

  def receive() = {
    case StateTick() => {}
  }
}