package com.github.Chronox.pool.actors
import com.github.Chronox.pool.{Global, Config}
import com.github.Chronox.pool.db.User

import akka.actor.{ Actor, ActorLogging }
import akka.http.scaladsl.Http
import akka.http.scaladsl.model._
import akka.stream.{ ActorMaterializer, ActorMaterializerSettings }
import akka.util.ByteString
import scala.concurrent.duration._
import scala.util.{ Failure, Success }
import HttpMethods._
import net.liftweb.json._
import java.sql.Timestamp
import java.time.LocalDateTime
import java.math.BigInteger

case class resetBestDeadline()
case class submitNonce(user: User, nonce: Long, deadline: BigInteger)
case class Result(message: String)
case class SubmitResult(result: String, deadline: String)

class DeadlineSubmitter extends Actor with ActorLogging {

  import akka.pattern.pipe
  import context.dispatcher 

  implicit val formats = DefaultFormats
  final implicit val materializer: ActorMaterializer = 
    ActorMaterializer(ActorMaterializerSettings(context.system))

  val http = Http(context.system)
  val baseSubmitURI = Uri(Config.NODE_ADDRESS + "/burst")

  def isBestDeadline(deadline: BigInteger): Boolean = {
    return deadline.compareTo(Global.currentBestDeadline) <= 0
  }

  def receive() = {
    case resetBestDeadline() => {
      Global.currentBestDeadline = Config.TARGET_DEADLINE
    }
    case submitNonce(user: User, nonce: Long, deadline: BigInteger) => {
      // If the given nonce is our best so far, send it to the network
      if(isBestDeadline(deadline)){
        val ent = FormData(Map("secretPhrase"->Config.SECRET_PHRASE,
          "accountId"->user.id.toString, "nonce"->nonce.toString)).toEntity
        http.singleRequest(
          HttpRequest(method = POST, uri = baseSubmitURI, entity = ent)
        ) onComplete {
          case Success(res: HttpResponse) => {
            // Check to see if the network calculated the same deadline as us
            val json = parse(res.entity.toString())
            json.extract[Result].message match {
              case Global.SUCCESS_MESSAGE => {  
                if (json.extract[SubmitResult].deadline == deadline) {
                  log.info("Deadline successfully submitted")
                  user.lastSubmitTime = Timestamp.valueOf(LocalDateTime.now())
                  user.lastSubmitHeight = Global.miningInfo.height.toLong
                  Global.currentBestDeadline = deadline
                  Global.poolStatistics.incrementValidNonces()

                  // Can convert bigint deadline to long because it's less than
                  // the target deadline
                  Global.shareManager ! addShare(user, 
                    Global.lastBlockInfo.block.toLong, nonce, 
                    deadline.longValue())
                  sender ! Result(Global.SUCCESS_MESSAGE)
                } else {
                  val e = "Response Deadline did not match calculated deadline"
                  log.error(e)
                  sender ! Result(e) 
                }
              }
              case (message: String) => {
                val e = "Network did not accept deadline"
                log.error(e)
                sender ! Result(e)
              }
            }
          }
          case Failure(error) => {
            log.error(error.toString())
            sender ! Result(error.toString())
          }
        }
      } else {
        log.info("Deadline successfully submitted")
        user.lastSubmitTime = Timestamp.valueOf(LocalDateTime.now())
        user.lastSubmitHeight = Global.miningInfo.height.toLong
        Global.poolStatistics.incrementValidNonces()

        // Can convert bigint deadline to long because it's less than
        // the target deadline
        Global.shareManager ! addShare(user, Global.lastBlockInfo.block.toLong,
          nonce, deadline.longValue())
        sender ! Result(Global.SUCCESS_MESSAGE)
      }
    }
  }
}