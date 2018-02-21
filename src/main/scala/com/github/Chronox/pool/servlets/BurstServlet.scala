package com.github.Chronox.pool.servlets

import com.github.Chronox.pool.Global

import akka.util.Timeout
import org.scalatra._
import scala.concurrent.duration._
import java.time.LocalDateTime
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

            //verify if the nonce is valid
            if(Global.deadlineChecker.verifyNonce(accountId, nonce)) {
              if(!(Global.userManager containsUser ip)){
                Global.userManager.addUser(ip, accountId)
              }
              // Check if deadline is best and submit it if it is
              // Update reward shares
            } else {
              Global.userManager.banUser(ip, LocalDateTime.now())
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