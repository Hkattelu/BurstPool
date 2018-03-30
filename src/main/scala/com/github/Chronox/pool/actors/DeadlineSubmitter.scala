package com.github.Chronox.pool.actors

import com.github.Chronox.pool.Global
import com.github.Chronox.pool.Config
import com.github.Chronox.pool.db.User

import akka.actor.{ Actor, ActorLogging }
import akka.http.scaladsl.Http
import akka.http.scaladsl.model._
import HttpMethods._
import akka.stream.{ ActorMaterializer, ActorMaterializerSettings }
import scala.util.{ Failure, Success }
import akka.util.ByteString
import scala.concurrent.duration._
import net.liftweb.json._

import java.math.BigInteger

case class resetBestDeadline()
case class submitNonce(user: User, nonce: Long, deadline: BigInteger)
case class SubmitResult(result: String, deadline: String)

class DeadlineSubmitter extends Actor with ActorLogging {

  import akka.pattern.pipe
  import context.dispatcher 

  implicit val formats = DefaultFormats
  final implicit val materializer: ActorMaterializer = 
    ActorMaterializer(ActorMaterializerSettings(context.system))

  val http = Http(context.system)
  val baseSubmitURI = (Config.NODE_ADDRESS + 
    "/burst?requestType=submitNonce&secretPhrase=" + Config.SECRET_PHRASE)

  def isBestDeadline(deadline: BigInteger): Boolean = {
    return deadline.compareTo(Global.currentBestDeadline) <= 0
  }

  def receive() = {
    case resetBestDeadline() => {
      Global.currentBestDeadline = Config.TARGET_DEADLINE
    }
    case submitNonce(user: User, nonce: Long, deadline: BigInteger) => {
      if(isBestDeadline(deadline)){
        http.singleRequest(
          HttpRequest(method = PUT,
           uri = baseSubmitURI+"&accountId="+user.id+"&nonce="+nonce)
        ) onComplete {
          case Success(res: HttpResponse) => {
            val result = parse(res.entity.toString()).extract[SubmitResult]
            if(deadline == (new BigInteger(result.deadline, 10))){
              log.info("Deadline successfully submitted")
              if(deadline.compareTo(Global.currentBestDeadline) <= 0){
                Global.currentBestDeadline = deadline
                Global.rewardManager ! addShare(user, 
                  new BigInteger(Global.miningInfo.block, 10), nonce, deadline)
              }
            } else {
              log.error("Response Deadline did not match calculated deadline")
            }
          }
          case Failure(error) => log.error(error.toString())
        }
      }
    }
  }
}