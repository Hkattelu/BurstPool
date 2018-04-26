import _root_.akka.actor.{Props, ActorSystem}
import com.github.Chronox.pool.{Global, Config}
import com.github.Chronox.pool.actors._
import com.github.Chronox.pool.servlets._
import com.github.Chronox.pool.db._
import org.scalatra._
import javax.servlet.ServletContext

class ScalatraBootstrap extends LifeCycle with DatabaseInit {
  val system = ActorSystem()

  override def init(context: ServletContext) {
    if(!Config.init()) return
    configureDb()

    Global.burstPriceChecker = 
      system.actorOf(Props[BurstPriceChecker], name = "BurstPriceChecker")
    Global.miningInfoUpdater = 
      system.actorOf(Props[MiningInfoUpdater], name = "MiningInfoUpdater")
    Global.deadlineSubmitter = 
      system.actorOf(Props[DeadlineSubmitter], name = "DeadlineSubmitter")
    Global.deadlineChecker = 
      system.actorOf(Props[DeadlineChecker], name = "DeadlineChecker")
    Global.userManager = 
      system.actorOf(Props[UserManager], name = "UserManager")
    Global.shareManager = 
      system.actorOf(Props[ShareManager], name = "ShareManager")
    Global.rewardPayout = 
      system.actorOf(Props[RewardPayout], name = "RewardPayout")
    Global.stateUpdater = 
      system.actorOf(Props(new StateUpdater(false)), name="StateUpdater")
    context.mount(new PoolServlet(), "/*")
    context.mount(new BurstPriceServlet(), "/getBurstPrice")
    context.mount(
      new BurstServlet(system, system.actorOf(Props[SubmissionHandler],
      name="SubmissionHandler")), "/burst")
  }

  override def destroy(context: ServletContext) {
    system.terminate()
    closeDbConnection()
  }
}