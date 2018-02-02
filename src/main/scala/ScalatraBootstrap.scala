import _root_.akka.actor.{Props, ActorSystem}
import com.github.Chronox.pool._
import org.scalatra._
import javax.servlet.ServletContext

class ScalatraBootstrap extends LifeCycle {

  val system = ActorSystem()
  val burstChecker = system.actorOf(Props[BURSTChecker])

  override def init(context: ServletContext) {
    context.mount(new PoolServlet(system), "/*") 
    context.mount(new BurstPriceServlet(system, burstChecker), "/updateBurstPrice")
  }

  override def destroy(context: ServletContext) {
    system.terminate()
  }
}