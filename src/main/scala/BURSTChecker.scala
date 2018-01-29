import akka.actor.{ Actor, ActorLogging }
import akka.http.scaladsl.Http
import akka.http.scaladsl.model._
import akka.stream.{ ActorMaterializer, ActorMaterializerSettings }
import akka.util.ByteString
import scala.util.parsing.json._
import scala.concurrent.duration._

case class StateTick()

class BURSTChecker extends Actor
  with ActorLogging {

  import akka.pattern.pipe
  import context.dispatcher

  final implicit val materializer: ActorMaterializer = 
    ActorMaterializer(ActorMaterializerSettings(context.system))

  val http = Http(context.system)
  val coinIndexAPIkey = "xlFeLy6SMAC3kg42aUz84cAVNCAWAR"

  override def preStart() = {
    context.system.scheduler.schedule(0 seconds, 10 seconds, self, StateTick())
  }

  def receive = {
    case StateTick() => {
      http.singleRequest(HttpRequest(
        uri = "https://api.coinmarketcap.com/v1/ticker/burst/")).pipeTo(self)
    }
    case HttpResponse(StatusCodes.OK, headers, entity, _) =>
      entity.dataBytes.runFold(ByteString(""))(_ ++ _).foreach { body =>
        {
          val BurstInfo = JSON.parseFull(body.utf8String).getOrElse(
              List(Map("price_usd" -> "Not Found", "price_btc" -> "Not Found"))
              ).asInstanceOf[List[Map[String, String]]].lift(0)
          log.info("Burst price in USD: " + (BurstInfo get "price_usd"))
          log.info("Burst price in BTC: " + (BurstInfo get "price_btc"))          
        }
      } 
    case resp @ HttpResponse(code, _, _, _) =>
      log.info("Request failed, response code: " + code)
      resp.discardEntityBytes()
  }
}