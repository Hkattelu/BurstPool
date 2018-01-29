import _root_.akka.actor.{Props, ActorSystem}
import com.github.Chronox.pool._
import org.scalatra._
import javax.servlet.ServletContext

class ScalatraBootstrap extends LifeCycle {

  val system = ActorSystem()
  val myActor = system.actorOf(Props[Myself])

  override def init(context: ServletContext) {
    context.mount(new PoolServlet(system), "/*") 
  }
}