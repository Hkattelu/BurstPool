import _root_.akka.actor.{Props, ActorSystem}
import com.github.Chronox.pool.{Global, Config}
import com.github.Chronox.pool.actors._
import com.github.Chronox.pool.servlets._
import com.github.Chronox.pool.db._
import net.liftweb.json._
import org.scalatra._
import scalaj.http.Http
import javax.servlet.ServletContext

class ScalatraBootstrap extends LifeCycle with DatabaseInit {

  implicit val formats = DefaultFormats
  val system = ActorSystem()
  Global.stateUpdater = system.actorOf(Props[StateUpdater])
  Global.burstPriceChecker = system.actorOf(Props[BurstPriceChecker])
  Global.miningInfoUpdater = system.actorOf(Props[MiningInfoUpdater])
  Global.deadlineSubmitter = system.actorOf(Props[DeadlineSubmitter])
  Global.deadlineChecker = system.actorOf(Props[DeadlineChecker])
  Global.userManager = system.actorOf(Props[UserManager])
  Global.shareManager = system.actorOf(Props[ShareManager])
  Global.rewardPayout = system.actorOf(Props[RewardPayout])

  override def init(context: ServletContext) {
    if(!Config.init()) return
    val getBlockURI = Config.NODE_ADDRESS + "/burst"
    val response = scalaj.http.Http(getBlockURI)
      .param("requestType", "getBlock").asString.body
    Global.miningInfo = parse(response).extract[Global.MiningInfo]
    configureDb()
    context.mount(new PoolServlet(), "/*")
    context.mount(new BurstPriceServlet(), "/getBurstPrice")
    context.mount(new BurstServlet(system), "/burst")
  }

  override def destroy(context: ServletContext) {
    system.terminate()
    closeDbConnection()
  }
}