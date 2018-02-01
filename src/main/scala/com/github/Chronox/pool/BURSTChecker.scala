package com.github.Chronox.pool

import akka.actor.{ Actor, ActorLogging }
import akka.http.scaladsl.Http
import akka.http.scaladsl.model._
import akka.stream.{ ActorMaterializer, ActorMaterializerSettings }
import akka.util.ByteString
import scala.concurrent.duration._
import net.liftweb.json._

case class StateTick()
case class getBurstPriceInfo()
case class BurstPriceInfo(price_usd: String, price_btc: String)

class BURSTChecker extends Actor
  with ActorLogging {

  import akka.pattern.pipe
  import context.dispatcher

  implicit val formats = DefaultFormats
  final implicit val materializer: ActorMaterializer = 
    ActorMaterializer(ActorMaterializerSettings(context.system))

  val http = Http(context.system)
  val coinIndexAPIkey = "xlFeLy6SMAC3kg42aUz84cAVNCAWAR"

  var BurstInfo : BurstPriceInfo = BurstPriceInfo("Not found", "Not found")

  override def preStart() = {
    context.system.scheduler.schedule(0 seconds, 10 seconds, self, StateTick())
  }

  def receive() = {
    case StateTick() => {
      http.singleRequest(HttpRequest(
        uri = "https://api.coinmarketcap.com/v1/ticker/burst/")).pipeTo(self)
    }
    case HttpResponse(StatusCodes.OK, headers, entity, _) =>
      entity.dataBytes.runFold(ByteString(""))(_ ++ _).foreach { body =>
        {
          BurstInfo = parse(body.utf8String).extract[BurstPriceInfo]  
        }
      } 
    case getBurstPriceInfo() => {
      sender() ! BurstInfo
    }
    case resp @ HttpResponse(code, _, _, _) =>
      log.info("Request failed, response code: " + code)
      resp.discardEntityBytes()
  }
}