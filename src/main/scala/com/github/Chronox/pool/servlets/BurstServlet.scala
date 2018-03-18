package com.github.Chronox.pool.servlets

import com.github.Chronox.pool.Global
import com.github.Chronox.pool.Config

import akka.util.Timeout
import org.scalatra._
import scala.concurrent.duration._
import java.time.LocalDateTime
import org.json4s.{DefaultFormats, Formats}
import org.scalatra.json._ 

import java.lang.Long

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
            val accId = Long.parseUnsignedLong(params("accountId"))
            val nonce = Long.parseUnsignedLong(params("nonce"))
            val deadline = Global.deadlineChecker.verifyNonce(accId, nonce)

            if(deadline.compareTo(Config.TARGET_DEADLINE) <= 0) {
              if(!(Global.userManager containsUser ip))
                Global.userManager.addUser(ip, accId)

              if(Global.deadlineChecker isBestDeadline deadline){
                Global.deadlineSubmitter.submitNonce(accId, nonce, deadline)
                Global.rewardManager.updateRewardShares()
              }
            } else {
              Global.userManager.banUser(ip, LocalDateTime.now())
            }
            // Update statistics
            Global.poolStatistics.updateStatistics()
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