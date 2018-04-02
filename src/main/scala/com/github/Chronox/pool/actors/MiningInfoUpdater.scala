package com.github.Chronox.pool.actors

import com.github.Chronox.pool.Global
import com.github.Chronox.pool.Config

import akka.actor.{ Actor, ActorLogging }
import akka.http.scaladsl.Http
import akka.http.scaladsl.model._
import akka.stream.{ ActorMaterializer, ActorMaterializerSettings }
import akka.util.ByteString
import scala.concurrent.duration._
import net.liftweb.json._
import java.time.LocalDateTime
import java.math.BigInteger

case class getNewBlock()
case class MiningInfo(generationSignature:String, block: String,
  baseTarget:String, height: Long, blockReward: String,
  generator: String, generatorRS: String,
  numberOfTransactions: String)

class MiningInfoUpdater extends Actor with ActorLogging {

  import akka.pattern.pipe
  import context.dispatcher 

  implicit val formats = DefaultFormats
  final implicit val materializer: ActorMaterializer = 
    ActorMaterializer(ActorMaterializerSettings(context.system))

  val http = Http(context.system)
  val burstRequest = "/burst?requestType="
  val getBlockURI = Config.NODE_ADDRESS + burstRequest + "getBlock"

  def receive() = {
    case getNewBlock() => {
      http.singleRequest(HttpRequest(uri = getBlockURI)).pipeTo(self)
    }
    case HttpResponse(StatusCodes.OK, headers, entity, _) =>
      entity.dataBytes.runFold(ByteString(""))(_ ++ _).foreach { body =>
        {
          val temp = parse(body.utf8String).extract[MiningInfo]
          if(temp.generationSignature != Global.miningInfo.generationSignature){
            if(temp.generator == Config.ACCOUNT_ID){
              Global.shareManager ! queueCurrentShares(
                new BigInteger(temp.block))
            } else {
              Global.shareManager ! dumpCurrentShares()
            }
            Global.miningInfo = temp
            Global.deadlineSubmitter ! resetBestDeadline()
          }
        }
      } 
    case resp @ HttpResponse(code, _, _, _) =>
      log.error("Request failed, response code: " + code)
      resp.discardEntityBytes()
  }
}