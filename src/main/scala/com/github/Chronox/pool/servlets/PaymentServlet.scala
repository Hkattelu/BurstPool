package com.github.Chronox.pool.servlets
import com.github.Chronox.pool.actors._

import akka.actor.{Actor, ActorRef, ActorSystem}
import akka.pattern.ask
import akka.util.Timeout
import org.scalatra.{Accepted, FutureSupport, ScalatraServlet}
import scala.concurrent.{Future, ExecutionContext, Await}
import scala.concurrent.duration._

import org.scalatra._
import org.json4s.{DefaultFormats, Formats}
import org.scalatra.json._
import java.math.BigInteger

class PaymentServlet(system: ActorSystem, paymentLedger: ActorRef) 
extends ScalatraServlet with JacksonJsonSupport with FutureSupport {

  protected implicit def executor: ExecutionContext = system.dispatcher
  protected implicit lazy val jsonFormats: Formats =
   DefaultFormats.withBigDecimal
  protected implicit val timeout: Timeout = new Timeout(5 seconds)
  protected val duration = 5 seconds

  before() {
    contentType = formats("json")
  }

  get("/"){
    if (params contains "account") {
      val id = new BigInteger(params("account")).longValue()
      new AsyncResult() {
        val is = (paymentLedger ? getUserPayment(id))(duration)
      }
    } else {
      new AsyncResult() {
        val is = (paymentLedger ? getPayments())(duration)
      }
    }
  }
}