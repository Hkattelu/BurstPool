import _root_.akka.actor.{Props, ActorSystem}
import com.github.Chronox.pool._
import com.github.Chronox.pool.actors._
import com.github.Chronox.pool.servlets._
import com.github.Chronox.pool.db._
import org.scalatra._
import javax.servlet.ServletContext

class ScalatraBootstrap extends LifeCycle with DatabaseInit {

  val system = ActorSystem()
  Global.stateUpdater = system.actorOf(Props[StateUpdater])
  Global.burstPriceChecker = system.actorOf(Props[BurstPriceChecker])
  Global.miningInfoUpdater = system.actorOf(Props[MiningInfoUpdater])
  Global.deadlineSubmitter = system.actorOf(Props[DeadlineSubmitter])
  Global.deadlineChecker = system.actorOf(Props[DeadlineChecker])
  Global.userManager = system.actorOf(Props[UserManager])
  Global.rewardManager = system.actorOf(Props[RewardManager])

  override def init(context: ServletContext) {
    if(!Config.init()) return
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