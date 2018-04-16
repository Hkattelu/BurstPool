package com.github.Chronox.pool.servlets

import com.github.Chronox.pool.Global
import com.github.Chronox.pool.Config
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

class BurstServlet(system: ActorSystem) extends ScalatraServlet
with JacksonJsonSupport with FutureSupport {

  protected implicit def executor: ExecutionContext = system.dispatcher
  protected implicit lazy val jsonFormats: Formats =
   DefaultFormats.withBigDecimal
  protected implicit val timeout: Timeout = 5 seconds

  before() {
    contentType = formats("json")
  }

  get("/"){
    try {
      params("requestType") match {
        case "submitNonce" => {"Submitting nonces only takes POST requests"}
        case "getMiningInfo" => Global.miningInfo
      }
    } catch {
      case e: NoSuchElementException => {
        response.setStatus(400)
        "Invalid request type"
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

            // Add user if we haven't seen this IP before
            val userFuture = (Global.userManager ? addUser(ip, accId))
              .mapTo[Option[User]]
            Await.result(userFuture, timeout.duration) match {
              case Some(user: User) => {
                val recipientFuture = (Global.userManager ? 
                  checkRewardRecipient(accId.toString)).mapTo[Boolean]
                if (Await.result(recipientFuture, timeout.duration)) {
                  var deadline: BigInteger = 
                    Config.TARGET_DEADLINE.add(BigInteger.valueOf(1L))
                  val deadlineFuture = (Global.deadlineChecker ? nonceToDeadline(
                    accId, nonce)).mapTo[BigInteger]
                  deadline = Await.result(deadlineFuture, timeout.duration) 
                  if(deadline.compareTo(Config.TARGET_DEADLINE) <= 0) {
                    // Submit nonce to network to verify
                    val submitFuture = (Global.deadlineSubmitter ? submitNonce(
                      user, nonce, deadline)).mapTo[Result]
                    Await.result(submitFuture, timeout.duration) match {
                      case Result(Global.SUCCESS_MESSAGE) => {
                        Global.userManager ! updateSubmitTime(ip)
                        "Deadline submission success:" + deadline.toString
                      }
                      case Result(message) => {
                        response.setStatus(500)
                        "Deadline submission failure: " + message
                      }
                    }
                  } else {
                    Global.userManager ! banUser(ip,
                      LocalDateTime.now().plusMinutes(Config.BAN_TIME))
                    Global.poolStatistics.incrementBadNonces()
                    response.setStatus(500)
                    "You submitted a bad deadline, and are temporarily banned" + 
                    " for " + Config.BAN_TIME + " minutes."
                  }
                } else {
                  response.setStatus(500)
                  "You cannot submit nonces unless you reward recipient is" +
                  " set to " + Config.ACCOUNT_ID + ". This can take up to 5" +
                  " blocks to update"
                }
              }
              case None => "You can't submit nonces either because"
            }
          } catch {
            case e: NoSuchElementException => {
              response.setStatus(400)
              "No account ID or nonce provided"
            }
          }
        }
        case "getMiningInfo" => "Getting mining info only takes GET requests"
      }
    } catch {
      case e: NoSuchElementException => {
        response.setStatus(400)
        "Invalid request type"
      }
    }
  }
}