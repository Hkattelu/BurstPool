import _root_.akka.actor.{Props, ActorSystem}
import com.github.Chronox.pool._
import org.scalatra._
import javax.servlet.ServletContext

class ScalatraBootstrap extends LifeCycle with DatabaseInit {

  val system = ActorSystem()
  Global.stateUpdater = system.actorOf(Props[StateUpdater])
  Global.burstChecker = system.actorOf(Props[BURSTChecker])
  Global.lastBlockGetter = system.actorOf(Props[LastBlockGetter])
  Global.userManager = system.actorOf(Props[UserManager])
  
  override def init(context: ServletContext) {
    configureDb()
    context.mount(new PoolServlet(), "/*")
    context.mount(new BurstPriceServlet(), "/getBurstPrice")
    context.mount(new BurstServlet(), "/burst")
  }

  override def destroy(context: ServletContext) {
    system.terminate()
    closeDbConnection()
  }
}