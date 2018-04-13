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
      val requestType = params("requestType")
      requestType match {
        case "submitNonce" => {
          try {
            val ip = request.getRemoteAddr()
            val accId = new BigInteger(params("accountId")).longValue()
            val nonce = new BigInteger(params("nonce")).longValue()
            var deadline: BigInteger = 
              Config.TARGET_DEADLINE.add(BigInteger.valueOf(1L))
            val deadlineFuture = (
              Global.deadlineChecker ? nonceToDeadline(accId, nonce))
              .mapTo[BigInteger]
            deadline = Await.result(deadlineFuture, timeout.duration)
            response.getWriter.println(
              "acc: " + accId + ", nonce: " + nonce)
            response.getWriter.println("deadline:" + deadline)
            if(deadline.compareTo(Config.TARGET_DEADLINE) <= 0) {
              // Add user if we haven't seen this IP before
              val userFuture = (Global.userManager ? addUser(ip, accId))
              userFuture.mapTo[Option[User]] onSuccess {
                case Some(user) => {
                  // Submit nonce if it is better than the pool's current best
                  if(deadline.compareTo(Global.currentBestDeadline) <= 0)
                    Global.deadlineSubmitter ! submitNonce(
                      user, nonce, deadline)
                  Global.poolStatistics.incrementValidNonces()
                  Global.userManager ! updateSubmitTime(ip)
                  response.getWriter.println("Deadline submission successful")
                  response.getWriter.println("Deadline: " + deadline.toString())
                }
                // User is banned, just return an error
                case None => response.getWriter.println(
                  "You are currently banned and cannot submit nonces.")
              }
            } else {
              Global.userManager ! banUser(ip,
                LocalDateTime.now().plusMinutes(Config.BAN_TIME))
              Global.poolStatistics.incrementBadNonces()
              response.setStatus(500)
              response.getWriter.println(
                "You submitted a bad deadline, and are now temporarily banned")
              response.getWriter.println("Deadline: " + deadline.toString())
            }
          } catch {
            case e: NoSuchElementException => {
              response.setStatus(400)
              response.getWriter().println("No account ID or nonce provided")
            }
          }
        }
        case "getMiningInfo" => Global.miningInfo
      }
    } catch {
      case e: NoSuchElementException => "Invalid request type"
    }
  }
}