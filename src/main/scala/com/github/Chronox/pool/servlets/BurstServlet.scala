package com.github.Chronox.pool.servlets
import com.github.Chronox.pool.{Global, Config}
import com.github.Chronox.pool.actors._
import com.github.Chronox.pool.db.{User, DatabaseSessionSupport}

import akka.actor.{Actor, ActorRef, ActorSystem}
import akka.pattern.ask
import akka.util.Timeout
import org.scalatra.{Accepted, FutureSupport, ScalatraServlet}
import scala.concurrent.{Future, ExecutionContext, Await}
import scala.concurrent.duration._

import org.scalatra._
import org.json4s.{DefaultFormats, Formats}
import org.scalatra.json._
import javax.servlet.http.HttpServletRequest
import java.lang.Long
import java.math.BigInteger

class BurstServlet(system: ActorSystem, submissionHandler: ActorRef) 
extends ScalatraServlet with JacksonJsonSupport with FutureSupport 
with DatabaseSessionSupport {

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
        case "getBlock" => Global.lastBlockInfo
        case "getPoolInfo" => Global.poolInfo
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
            var miner_type: Option[String] = getMinerType(request)

            new AsyncResult() {    
              val is = (submissionHandler ? requestSubmission(
                ip, accId, nonce, miner_type, response))(5 seconds)
            }
          } catch {
            case e: NoSuchElementException => {
              response.setStatus(400)
              "No account ID or nonce provided"
            }
          }
        }
        case "getMiningInfo" => "Getting mining info only takes GET requests"
        case "getBlock" => "Getting last Block only takes GET requests"
        case "getPoolInfo" => "Getting pool info only takes GET requests"
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

  def getMinerType(implicit request: HttpServletRequest) : Option[String] = {
    var miner_type: Option[String] = None
    if (params contains "X-Miner")
      miner_type = Some(params("X-Miner"))
    else if (params contains "miner")
      miner_type = Some(params("miner"))
    else if (params contains "secretPhrase"){
      if (params("secretPhrase") == "cryptoport")
        miner_type = Some("uray")
      else if (params("secretPhrase") == "HereGoesTheSecret")
        miner_type = Some("java")
      else if (params("secretPhrase") == "pool-mining")
        miner_type = Some("poolmining")
    }
    else if (params contains "deadline")
      miner_type = Some("Blago")
    return miner_type
  }
}