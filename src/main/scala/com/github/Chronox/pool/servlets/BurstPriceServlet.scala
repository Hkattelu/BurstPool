package com.github.Chronox.pool.servlets

import com.github.Chronox.pool.Global

import akka.util.Timeout
import org.scalatra._
import scala.concurrent.duration._
import org.json4s.{DefaultFormats, Formats}
import org.scalatra.json._

class BurstPriceServlet extends ScalatraServlet with JacksonJsonSupport {

  protected implicit lazy val jsonFormats: Formats = 
    DefaultFormats.withBigDecimal
  protected implicit val timeout: Timeout = 5 seconds

  before() {
    contentType = formats("json")
  }

  get("/") {
    Global.burstPriceInfo
  }
}