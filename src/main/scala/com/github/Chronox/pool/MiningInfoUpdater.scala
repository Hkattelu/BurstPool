package com.github.Chronox.pool

import akka.actor.{ Actor, ActorLogging }
import akka.http.scaladsl.Http
import akka.http.scaladsl.model._
import akka.stream.{ ActorMaterializer, ActorMaterializerSettings }
import akka.util.ByteString
import scala.concurrent.duration._
import net.liftweb.json._

case class getNewBlock()
case class MiningInfo(generationSignature:String,
  baseTarget:String, height:String, targetDeadline:String)

class MiningInfoUpdater extends Actor with ActorLogging {

  import akka.pattern.pipe
  import context.dispatcher

  implicit val formats = DefaultFormats
  final implicit val materializer: ActorMaterializer = 
    ActorMaterializer(ActorMaterializerSettings(context.system))

  val http = Http(context.system)
  val getBlockURI = Config.NODE_ADDRESS + "/burst?requestType=getBlock"

  def receive() = {
    case getNewBlock() => {
      http.singleRequest(HttpRequest(uri = getBlockURI)).pipeTo(self)
    }
    case HttpResponse(StatusCodes.OK, headers, entity, _) =>
      entity.dataBytes.runFold(ByteString(""))(_ ++ _).foreach { body =>
        {
          Global.miningInfo = parse(body.utf8String).extract[MiningInfo]
        }
      } 
    case resp @ HttpResponse(code, _, _, _) =>
      log.info("Request failed, response code: " + code)
      resp.discardEntityBytes()
  }
}