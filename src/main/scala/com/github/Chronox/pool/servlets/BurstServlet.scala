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

import scala.concurrent.ExecutionContext
import scala.concurrent.duration._

import org.scalatra._
import scala.concurrent.duration._
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
            val future = Global.deadlineChecker ? nonceToDeadline(accId, nonce)
            future onComplete {
              case Success(result) => {deadline = result.asInstanceOf[BigInteger]}
              case Failure(error) => {println(error.toString())}
            }
            if(deadline.compareTo(Config.TARGET_DEADLINE) <= 0) {
              if(!(Global.userManager containsUser ip))
                Global.userManager.addUser(ip, accId)
              val user: User = Global.userManager.getUser(ip)
              Global.poolStatistics.incrementValidNonces()
              if(deadline.compareTo(Global.currentBestDeadline) <= 0)
                Global.deadlineSubmitter ! SubmitNonce(accId, nonce, deadline)
            } else {
              Global.userManager.banUser(ip, LocalDateTime.now())
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