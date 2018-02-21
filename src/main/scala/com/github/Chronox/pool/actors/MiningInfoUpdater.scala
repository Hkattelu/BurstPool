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

case class getNewBlock()
case class updateBlockChainStatus()
case class MiningInfo(generationSignature:String,
  baseTarget:String, height: Long, blockReward: String,
  generator: String, generatorRS: String,
  numberOfTransactions: Long, timeStamp: Long)
case class Difficulty(cumulativeDifficulty: String)

class MiningInfoUpdater extends Actor with ActorLogging {

  import akka.pattern.pipe
  import context.dispatcher 

  implicit val formats = DefaultFormats
  final implicit val materializer: ActorMaterializer = 
    ActorMaterializer(ActorMaterializerSettings(context.system))

  val http = Http(context.system)
  val burstRequest = "/burst?requestType="
  val getBlockURI = Config.NODE_ADDRESS + burstRequest + "getBlock"
  val getStatusURI = Config.NODE_ADDRESS + burstRequest + "getBlockChainStatus"

  def receive() = {
    case getNewBlock() => {
      http.singleRequest(HttpRequest(uri = getBlockURI)).pipeTo(self)
    }
    case updateBlockChainStatus() => {
      http.singleRequest(HttpRequest(uri = getStatusURI)).pipeTo(self)      
    }
    case HttpResponse(StatusCodes.OK, headers, entity, _) =>
      entity.dataBytes.runFold(ByteString(""))(_ ++ _).foreach { body =>
        {
          val jsonResponse = parse(body.utf8String)
          jsonResponse \ "cumulativeDifficulty" match {
            case JString(difficulty) => {
              Global.difficulty = jsonResponse.extract[Difficulty]
            }
            case _ => Global.miningInfo = jsonResponse.extract[MiningInfo]  
          }
        }
      } 
    case resp @ HttpResponse(code, _, _, _) =>
      log.info("Request failed, response code: " + code)
      resp.discardEntityBytes()
  }
}