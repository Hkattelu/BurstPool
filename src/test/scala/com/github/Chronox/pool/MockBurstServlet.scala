package com.github.Chronox.pool

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
import java.math.BigInteger

class MockBurstServlet(system: ActorSystem) extends ScalatraServlet 
with JacksonJsonSupport with FutureSupport {

  protected implicit def executor: ExecutionContext = system.dispatcher
  protected implicit lazy val jsonFormats: Formats =
   DefaultFormats.withBigDecimal
  protected implicit val timeout: Timeout = 3 seconds

  before() {
    contentType = formats("json")
  }

  get("/"){
    try {
      println(request.toString)
      params("requestType") match {
        case "submitNonce" => "Submitting nonces only takes POST requests"
        case "sendMoney" => "Sending money only takes POST requests"
        // Return dummy info
        case "getMiningInfo" => Global.MiningInfo("84", "1", "1")
        case "getBlock" => {
          new BlockResponse("99", "1", params("block"))
        }
      }
    } catch {
      case e: NoSuchElementException => {
        response.setStatus(400)
        "Missing requestType parameter"
      }
    }
  }

  post("/") {
    try {
      params("requestType") match {
        // Return a success response, no matter the nonce
        case "submitNonce" => {
          val secret = params("secretPhrase")
          val accId = new BigInteger(params("accountId")).longValue()
          val nonce = new BigInteger(params("nonce")).longValue()
          val deadlineFuture = (Global.deadlineChecker ? nonceToDeadline(
            accId, nonce)).mapTo[BigInteger]
          val deadline = Await.result(deadlineFuture, timeout.duration).toString
          SubmitResult(Global.SUCCESS_MESSAGE, deadline)
        }
        // Validate and broadcast tx if recipient = 1
        // Validate but not broadcast tx if recipient = 0
        // Throw error if recipient is anything else
        case "sendMoney" => {
          val deadline = params("deadline")
          val recipient = params("recipient")
          val amountNQT = params("amountNQT")
          val feeNQT = params("feeNQT")
          val secret = params("secretPhrase")
          if (recipient == "1")
            TransactionResponse("1", true)
          else if (recipient == "0")
            TransactionResponse("1", false)
          else 
            Global.ErrorMessage("3", "some error")
        }
        // Return dummy info
        case "getMiningInfo" => "Getting mining info only takes GET requests"
        case "getBlock" => "Getting blocks only takes GET requests"
      }
    } catch {
      case e: NoSuchElementException => {
        response.setStatus(400)
        "Missing requestType parameter"
      }
    }
  }
}