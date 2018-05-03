package com.github.Chronox.pool.servlets
import com.github.Chronox.pool.{Global, PoolStatistics}
import com.github.Chronox.pool.actors._

import org.scalatra.{Accepted, FutureSupport, ScalatraServlet}
import scala.concurrent.{Future, ExecutionContext, Await}
import scala.concurrent.duration._

import akka.actor.ActorSystem
import akka.pattern.ask
import org.scalatra._
import org.json4s.JsonDSL._
import org.json4s._
import org.scalatra.atmosphere._
import org.scalatra.json.{JValueResult, JacksonJsonSupport}
import java.math.BigInteger

class InterfactUpdateServlet(system: ActorSystem) extends ScalatraServlet 
with JacksonJsonSupport with SessionSupport with AtmosphereSupport {

  protected implicit lazy val jsonFormats: Formats =
   DefaultFormats.withBigDecimal
  override implicit protected def scalatraActorSystem = system
  protected val duration = 5 seconds

  before() {
    contentType = formats("json")
  }

  get("/statistics"){
    Global.poolStatistics.get()
  }

  get("/getBurstPrice") {
    Global.burstPriceInfo
  }

  get("/payments") {
    if (params contains "account") {
      val id = new BigInteger(params("account")).longValue()
      new AsyncResult() {
        val is = (Global.paymentLedger ? getUserPayment(id))(duration)
      }
    } else {
      new AsyncResult() {
        val is = (Global.paymentLedger ? getPayments())(duration)
      }
    }
  }

  get("/shares/current"){
    new AsyncResult() {
      val is = (Global.shareManager ? getCurrentPercents())(duration)
    }
  }

  get("/shares/historic"){
    new AsyncResult() {
      val is = (Global.shareManager ? getAverageHistoricalPercents())(duration)
    }
  }

}
