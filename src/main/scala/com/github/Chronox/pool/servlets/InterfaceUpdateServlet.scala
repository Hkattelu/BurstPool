package com.github.Chronox.pool.servlets
import com.github.Chronox.pool.{Global, PoolStatistics}
import com.github.Chronox.pool.actors._

import org.scalatra.{Accepted, FutureSupport, ScalatraServlet}
import scala.concurrent.{Future, ExecutionContext, Await}
import scala.concurrent.duration._

import akka.actor.ActorSystem
import akka.pattern.ask
import org.scalatra._
import org.scalatra.json._

import org.json4s.{DefaultFormats, Formats}
import java.math.BigInteger

class InterfaceUpdateServlet(system: ActorSystem) extends ScalatraServlet 
with JacksonJsonSupport {

  protected implicit def executor: ExecutionContext = system.dispatcher
  protected implicit lazy val jsonFormats: Formats =
   DefaultFormats.withBigDecimal
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
    new AsyncResult() {
      val is = (Global.paymentLedger ? getPayments())(duration)
    }  
  }

  get("/payments/:id") {
    val id = new BigInteger(params("id")).longValue()
    new AsyncResult() {
      val is = (Global.paymentLedger ? getUserPayment(id))(duration)
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
