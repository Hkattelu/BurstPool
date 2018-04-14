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
case class Result(result: String)
case class SubmitResult(result: String, deadline: String)

class DeadlineSubmitter extends Actor with ActorLogging {

  import akka.pattern.pipe
  import context.dispatcher 

  implicit val formats = DefaultFormats
  final implicit val materializer: ActorMaterializer = 
    ActorMaterializer(ActorMaterializerSettings(context.system))

  val http = Http(context.system)
  var baseSubmitURI = Uri(Config.NODE_ADDRESS + "/burst")

  def isBestDeadline(deadline: BigInteger): Boolean = 
    return deadline.compareTo(Global.currentBestDeadline) <= 0

  def receive() = {
    case resetBestDeadline() => {
      Global.currentBestDeadline = Config.TARGET_DEADLINE
    }
    case Global.setSubmitURI(uri: String) => baseSubmitURI = Uri(uri)
    case submitNonce(user: User, nonce: Long, deadline: BigInteger) => {
      val s = sender
      val ent = FormData(Map("secretPhrase"->Config.SECRET_PHRASE,
        "accountId"->user.id.toString, "nonce"->nonce.toString, 
        "requestType"->"submitNonce")).toEntity
      http.singleRequest(
        HttpRequest(method = POST, uri = baseSubmitURI, entity = ent)
      ) onComplete {
        case Success(res: HttpResponse) => {
          res.entity.dataBytes.runFold(ByteString(""))(_ ++ _).foreach { body =>
            val json = parse(body.utf8String)
            json.extract[Result].result match {
              case Global.SUCCESS_MESSAGE => {  
                // Check to see if the network calculated the same deadline
                if (json.extract[SubmitResult].deadline == deadline) {
                  log.info("Deadline successfully submitted")
                  user.lastSubmitTime = Timestamp.valueOf(LocalDateTime.now())
                  user.lastSubmitHeight = Global.miningInfo.height.toLong
                  Global.poolStatistics.incrementValidNonces()
                  if(isBestDeadline(deadline))
                    Global.currentBestDeadline = deadline
                  Global.shareManager ! addShare(user, 
                    Global.lastBlockInfo.block.toLong, nonce, 
                    deadline.longValue())
                  s ! Result(Global.SUCCESS_MESSAGE)
                } else {
                  val e = "Response Deadline did not match calculated deadline"
                  log.info(e)
                  s ! Result(e) 
                }
              }
              case _ => {
                val e = "Network did not accept deadline"
                log.info(e)
                s ! Result(e)
              }
            }
          } 
        }
        case Failure(error) => {
          log.error(error.toString())
          s ! Result(error.toString())
        }
      }
    }
  }
}