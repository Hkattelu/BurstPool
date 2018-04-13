package com.github.Chronox.pool.actors
import com.github.Chronox.pool.{Global, Config}

import akka.actor.{ Actor, ActorLogging }
import akka.http.scaladsl.Http
import akka.http.scaladsl.model._
import akka.stream.{ ActorMaterializer, ActorMaterializerSettings }
import akka.util.ByteString
import scala.concurrent.duration._
import net.liftweb.json._
import java.time.LocalDateTime
import java.math.BigInteger

case class getLastBlock()
case class getNewMiningInfo()

class MiningInfoUpdater extends Actor with ActorLogging {

  import akka.pattern.pipe
  import context.dispatcher 

  implicit val formats = DefaultFormats
  final implicit val materializer: ActorMaterializer = 
    ActorMaterializer(ActorMaterializerSettings(context.system))

  val http = Http(context.system)
  val burstRequest = Config.NODE_ADDRESS + "/burst?requestType="
  val getBlockURI = burstRequest + "getBlock"
  val getMiningInfoURI = burstRequest + "getMiningInfo"

  def receive() = {
    case getLastBlock() => {
      http.singleRequest(HttpRequest(uri = getBlockURI)).pipeTo(self)
    }
    case getNewMiningInfo() => {
      http.singleRequest(HttpRequest(uri = getMiningInfoURI)).pipeTo(self)
    }
    case HttpResponse(StatusCodes.OK, headers, entity, _) =>
      entity.dataBytes.runFold(ByteString(""))(_ ++ _).foreach { body =>
        {
          if(body.utf8String contains "generator") { 
            Global.lastBlockInfo = 
              parse(body.utf8String).extract[Global.LastBlockInfo]
            // Pay out shares if we mined the last block, dump them otherwise
            Global.lastBlockInfo.generator == Config.ACCOUNT_ID match {
              case true => Global.shareManager ! queueCurrentShares(
                Global.lastBlockInfo.block.toLong)
              case false => Global.shareManager ! dumpCurrentShares()
            }
          } else {
            val temp = parse(body.utf8String).extract[Global.MiningInfo]
            
            // Update information if there is a new block
            if(temp.generationSignature 
              != Global.miningInfo.generationSignature){
              Global.miningInfo = temp
              Global.deadlineSubmitter ! resetBestDeadline()
              self ! getLastBlock()
            }
          } 
        }
      } 
    case resp @ HttpResponse(code, _, _, _) =>
      log.error("Request failed, response code: " + code)
      resp.discardEntityBytes()
  }
}