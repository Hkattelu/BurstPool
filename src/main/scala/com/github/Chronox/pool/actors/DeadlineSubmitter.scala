package com.github.Chronox.pool.actors

import com.github.Chronox.pool.Global
import com.github.Chronox.pool.Config

import akka.actor.{ Actor, ActorLogging }
import akka.http.scaladsl.Http
import akka.http.scaladsl.model._
import HttpMethods._
import akka.stream.{ ActorMaterializer, ActorMaterializerSettings }
import akka.util.ByteString
import scala.concurrent.duration._
import net.liftweb.json._

import java.math.BigInteger

case class ResetBestDeadline()
case class SubmitNonce(accId: Long, nonce: Long, deadline: BigInteger)
case class SubmitResult(result: String, deadline: String)

class DeadlineSubmitter extends Actor with ActorLogging {

  import akka.pattern.pipe
  import context.dispatcher 

  implicit val formats = DefaultFormats
  final implicit val materializer: ActorMaterializer = 
    ActorMaterializer(ActorMaterializerSettings(context.system))

  val http = Http(context.system)
  val baseSubmitURI = (Config.NODE_ADDRESS + 
    "/burst?requestType=submitNonce&secretPhrase=cryptoport"

  def isBestDeadline(deadline: BigInteger): Boolean = {
    return deadline.compareTo(Global.currentBestDeadline) <= 0
  }

  def receive() = {
    case ResetBestDeadline() => {
      Global.currentBestDeadline = Config.TARGET_DEADLINE
    }
    case SubmitNonce(accountId: Long, nonce: Long, deadline: BigInteger) => {
      if(isBestDeadline(deadline)){
        http.singleRequest(
          HttpRequest(method = PUT,
           uri = baseSubmitURI+"&accountId="+accountId+"&nonce="+nonce)
        ) onComplete {
          case Success(res: HttpResponse) => {
            val result = res.entity.asString.parseJson.convertTo[SubmitResult]
            val responseDeadline = new BigInteger(result.deadline, 10)
            if(deadline == responseDeadline){
              log.info("Deadline successfully submitted")
              if(deadline.compareTo(Global.currentBestDeadline) <= 0){
                Global.currentBestDeadline = deadline
                Global.rewardManager.updateRewardShares(
                  accountId, new BigInteger(Global.miningInfo.block,10),
                  nonce, deadline)
              } else {
                log.error("Response Deadline did not match calculated deadline")
              }
            }
          }
          case Failure(error) => log.error(error)
        }
      }
    }
  }
}