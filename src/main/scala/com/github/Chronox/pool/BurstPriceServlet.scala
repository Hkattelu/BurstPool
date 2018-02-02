package com.github.Chronox.pool

import akka.actor.{Actor, ActorRef, ActorSystem}
import akka.pattern.ask
import akka.util.Timeout
import org.scalatra._
import scala.concurrent.{ExecutionContext, Future, Promise}
import scala.concurrent.duration._
import scala.util.{Failure, Success, Try}
import org.json4s.{DefaultFormats, Formats}
import org.scalatra.json._

class BurstPriceServlet extends ScalatraServlet with JacksonJsonSupport {

  protected implicit lazy val jsonFormats: Formats = DefaultFormats.withBigDecimal
  protected implicit val timeout: Timeout = 5 seconds

  before() {
    contentType = formats("json")
  }

  get("/") {
    Global.burstChecker ? getBurstPriceInfo()
  }
}