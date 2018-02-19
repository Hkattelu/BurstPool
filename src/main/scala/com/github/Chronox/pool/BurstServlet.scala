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
            val ip = request.getRemoteAddr()
            val nonce = params("nonce")
            val accountId = params("accountId")

            if(Global.deadlineSubmitter verifyNonce (accountId -> nonce)) {
              if(!(Global.userManager containsUser ip)){
                Global.userManager addUser (ip -> accountId)
              }
              // Update reward shares
              if(Global.deadlineSubmitter ? isBestNonce(ip, accountId, nonce)) {
                Global.deadlineSubmitter ! submitNonce(accountId, nonce)
              }
            } else {
              Global.userManager banUser ip
            }
            // Update statistics
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