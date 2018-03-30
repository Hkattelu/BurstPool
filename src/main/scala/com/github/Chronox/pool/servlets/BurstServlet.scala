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
import scala.concurrent.Future
import scala.concurrent.ExecutionContext
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
            val accId = Long.parseUnsignedLong(params("accountId"))
            val nonce = Long.parseUnsignedLong(params("nonce"))
            
            var deadline: BigInteger = 
              Config.TARGET_DEADLINE.add(BigInteger.valueOf(1L))
            val deadlineFuture: Future[Any] = 
              Global.deadlineChecker ? nonceToDeadline(accId, nonce)
            deadlineFuture onSuccess {
              case Some(result) => {
                deadline = result.asInstanceOf[BigInteger]
              }
            }
            if(deadline.compareTo(Config.TARGET_DEADLINE) <= 0) {
              Global.poolStatistics.incrementValidNonces()

              // Add user if we haven't seen this IP before
              val containsUserFuture: Future[Any] = 
                Global.userManager ? containsUser(ip)
              containsUserFuture onSuccess {
                case Some(result) => {
                  if(!result.asInstanceOf[Boolean])
                    Global.userManager ! addUser(ip, accId)
                }
              }

              // Get the user from the manager
              var user: User = null
              val userFuture: Future[Any] = Global.userManager ? getUser(ip)
              userFuture onSuccess {
                case Some(result) => {user = result.asInstanceOf[User]}
              }

              // Submit nonce if it is better than the pool's currentbest
              if(deadline.compareTo(Global.currentBestDeadline) <= 0)
                Global.deadlineSubmitter ! submitNonce(user, nonce, deadline)
                Global.userManager ! updateSubmitTime(ip)
            } else {
              Global.userManager ! banUser(ip,
                LocalDateTime.now().plusMinutes(Config.BAN_TIME))
              Global.poolStatistics.incrementBadNonces()
            }
          } catch {
            case e: NoSuchElementException => "No account ID or nonce provided"
          }
        }
        case "getMiningInfo" => Global.miningInfo
      }
    } catch {
      case e: NoSuchElementException => "Invalid request type"
    }
  }
}