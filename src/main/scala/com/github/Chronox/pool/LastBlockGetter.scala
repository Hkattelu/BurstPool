package com.github.Chronox.pool

import akka.actor.{ Actor, ActorLogging }
import akka.http.scaladsl.Http
import akka.http.scaladsl.model._
import akka.stream.{ ActorMaterializer, ActorMaterializerSettings }
import akka.util.ByteString
import scala.concurrent.duration._
import net.liftweb.json._

case class getNewBlock()

class LastBlockGetter extends Actor with ActorLogging {

  import akka.pattern.pipe
  import context.dispatcher

  implicit val formats = DefaultFormats
  final implicit val materializer: ActorMaterializer = 
    ActorMaterializer(ActorMaterializerSettings(context.system))

  val http = Http(context.system)
  val poolURI = 
    "http://pool.burstcoin.space:8124/burst?requestType=getMiningInfo"
  var lastBlock = "Dummy"

  def receive() = {
    case getNewBlock() => {
      http.singleRequest(HttpRequest(uri = poolURI)).pipeTo(self)
    }
    case HttpResponse(StatusCodes.OK, headers, entity, _) =>
      entity.dataBytes.runFold(ByteString(""))(_ ++ _).foreach { body =>
        {
          log.info(body.utf8String)
        }
      } 
    case resp @ HttpResponse(code, _, _, _) =>
      log.info("Request failed, response code: " + code)
      resp.discardEntityBytes()
  }
}