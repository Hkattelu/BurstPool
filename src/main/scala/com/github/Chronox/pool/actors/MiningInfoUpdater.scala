package com.github.Chronox.pool.actors
import com.github.Chronox.pool.{Global, Config}

import akka.actor.{ Actor, ActorLogging }
import akka.http.scaladsl.Http
import akka.http.scaladsl.model._
import akka.stream.{ ActorMaterializer, ActorMaterializerSettings }
import akka.util.ByteString
import scala.concurrent.duration._
import scala.util.{ Failure, Success }
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
    case _: getLastBlock => {
      http.singleRequest(HttpRequest(uri = getBlockURI)) onComplete {
        case Success(r: HttpResponse) => {
          if(r.status != StatusCodes.OK) {
            log.error("Request failed, response status: " + r.status)
            r.discardEntityBytes()
          } else { 
            r.entity.dataBytes.runFold(ByteString(""))(_ ++ _) foreach { body =>
              {
                Global.lastBlockInfo = 
                  parse(body.utf8String).extract[Global.LastBlockInfo]
                // Pay out shares if we mined last block, dump them otherwise
                Global.lastBlockInfo.generator == Config.ACCOUNT_ID match {
                  case true => Global.shareManager ! queueCurrentShares(
                    new BigInteger(Global.lastBlockInfo.block).longValue)
                  case false => Global.shareManager ! dumpCurrentShares()
                }
              }
            } 
          }
        }
        case Failure(e) => log.error("Failed to get Block Info: " + e.toString)
      }
    }
    case _: getNewMiningInfo => {
      http.singleRequest(HttpRequest(uri = getMiningInfoURI)) onComplete {
        case Success(r: HttpResponse) => {
          if(r.status != StatusCodes.OK) {
            log.error("Request failed, response status: " + r.status)
            r.discardEntityBytes()
          } else {
            r.entity.dataBytes.runFold(ByteString(""))(_ ++ _).foreach { body =>
              {   
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
        }
        case Failure(e) => log.error("Failed to get Mining Info: " + e.toString)
      }
    }
  }
}