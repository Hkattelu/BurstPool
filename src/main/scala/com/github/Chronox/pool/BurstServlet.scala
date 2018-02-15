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

class BurstServlet extends ScalatraServlet with JacksonJsonSupport {

  protected implicit lazy val jsonFormats: Formats =
   DefaultFormats.withBigDecimal
  protected implicit val timeout: Timeout = 5 seconds

  before() {
    contentType = formats("json")
  }

  get("/"){

    try {
      val requestType = params("requestType")
      requestType match {
        case "submitNonce" => {
          try {
            params("nonce")
            params("accountId")

            // verify nonce validitiy

            // if it is invalid, ban the IP temporarily

            // check if user is already in pool
            // if so, update the last submit time

            // if not, add a new user

            // check if this is the best deadline so far

            // if it is, submit it to a main node

            // otherwise, record it and change the current reward shares
          } catch {
            case e: NoSuchElementException => "No account ID or nonce provided"
          }
          println(request.toString())
        }
        case "getMiningInfo" => Global.miningInfo
      }
    } catch {
      case e: NoSuchElementException => "Invalid request type"
    }

  }
}