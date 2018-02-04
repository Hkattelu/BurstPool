package com.github.Chronox.pool

import akka.actor.{ Actor, ActorLogging }
import akka.pattern.ask
import akka.util.Timeout
import org.scalatra._
import scala.concurrent.{ExecutionContext, Future, Promise}
import scala.concurrent.duration._
import scala.util.{Failure, Success, Try}
import org.json4s.{DefaultFormats, Formats}
import org.scalatra.json._ 

case class submitBlock()

class DeadlineSubmitter extends Actor with ActorLogging {

  protected implicit lazy val jsonFormats: Formats = 
    DefaultFormats.withBigDecimal
  protected implicit val timeout: Timeout = 5 seconds

  def receive() = {
    case submitBlock() => {
      
    }
  }
}