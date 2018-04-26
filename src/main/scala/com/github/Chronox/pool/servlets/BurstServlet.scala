package com.github.Chronox.pool.servlets
import com.github.Chronox.pool.{Global, Config}
import com.github.Chronox.pool.actors._
import com.github.Chronox.pool.db.User

import akka.actor.{Actor, ActorRef, ActorSystem}
import akka.pattern.ask
import akka.util.Timeout
import org.scalatra.{Accepted, FutureSupport, ScalatraServlet}
import scala.util.{ Failure, Success }
import scala.concurrent.{Future, ExecutionContext, Await}
import scala.concurrent.duration._

import org.scalatra._
import java.time.LocalDateTime
import org.json4s.{DefaultFormats, Formats}
import org.scalatra.json._ 
import java.lang.Long
import java.math.BigInteger

class BurstServlet(system: ActorSystem, submissionHandler: ActorRef) 
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
    try {
      params("requestType") match {
        case "submitNonce" => "Submitting nonces only takes POST requests"
        case "getMiningInfo" => Global.miningInfo
        case "sendMoney" => "Money cannot be sent through this pool"
        case _ => {
          response.setStatus(400)
          "Invalid request type"
        }
      }
    } catch {
      case e: NoSuchElementException => {
        response.setStatus(400)
        "No request type given"
      }
    }
  }

  post("/"){
    try {
      params("requestType") match {
        case "submitNonce" => {
          try {
            val ip = request.getRemoteAddr()
            val accId = new BigInteger(params("accountId")).longValue()
            val nonce = new BigInteger(params("nonce")).longValue()
            
            new AsyncResult() {    
              val is = (submissionHandler ? requestSubmission(
                ip, accId, nonce, response))(5 seconds)
            }
          } catch {
            case e: NoSuchElementException => {
              response.setStatus(400)
              "No account ID or nonce provided"
            }
          }
        }
        case "getMiningInfo" => "Getting mining info only takes GET requests"
        case "sendMoney" => "Money cannot be sent through this pool"
        case _ => {
          response.setStatus(400)
          "Invalid request type"
        }
      }
    } catch {
      case e: NoSuchElementException => {
        response.setStatus(400)
        "No request type given"
      }
    }
  }
}