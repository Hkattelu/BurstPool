package com.github.Chronox.pool.servlets
import com.github.Chronox.pool.PoolStatistics

import org.scalatra.{Accepted, FutureSupport, ScalatraServlet}
import scala.concurrent.{Future, ExecutionContext, Await}
import scala.concurrent.duration._

import org.scalatra._
import org.json4s.{DefaultFormats, Formats}
import org.scalatra.json._

class StatisticsServlet extends ScalatraServlet with JacksonJsonSupport {

  protected implicit lazy val jsonFormats: Formats =
   DefaultFormats.withBigDecimal

  before() {
    contentType = formats("json")
  }

  get("/"){
    PoolStatistics.get()
  }
}