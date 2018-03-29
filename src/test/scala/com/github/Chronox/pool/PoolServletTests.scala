package com.github.Chronox.pool

import com.github.Chronox.pool._
import com.github.Chronox.pool.actors._
import com.github.Chronox.pool.servlets._
import com.github.Chronox.pool.db._
import akka.pattern.ask
import akka.util.Timeout
import scala.concurrent.Future
import scala.concurrent.ExecutionContext
import scala.concurrent.duration._
import scala.util.{ Failure, Success }
import org.scalatra.test.scalatest._
import java.time.LocalDateTime
import org.scalatest.FunSuiteLike
import _root_.akka.actor.{Props, ActorSystem}
import javax.servlet.ServletContext

class PoolServletTests extends ScalatraSuite with FunSuiteLike{


  Config.init()
  val system = ActorSystem()
  Global.stateUpdater = system.actorOf(Props[StateUpdater])
  Global.burstPriceChecker = system.actorOf(Props[BurstPriceChecker])
  Global.miningInfoUpdater = system.actorOf(Props[MiningInfoUpdater])
  Global.deadlineSubmitter = system.actorOf(Props[DeadlineSubmitter])
  Global.deadlineChecker = system.actorOf(Props[DeadlineChecker])
  Global.userManager = system.actorOf(Props[UserManager])

  addServlet(classOf[PoolServlet], "/*")
  addServlet(classOf[BurstPriceServlet], "/getBurstPrice")
  addServlet(new BurstServlet(system), "/burst")

  protected implicit def executor: ExecutionContext = system.dispatcher
  protected implicit val timeout: Timeout = 2 seconds
  
  test("All servlets up and running"){
    get("/"){
      status should equal (200)
    }

    get("/getBurstPrice"){
      status should equal (200)
    }

    get("/burst"){
      status should equal (200)
    }
  }

  test("Getting Burst price"){
    get("/getBurstPrice"){
      status should equal (200)
      body should include ("price_usd")
      body should include ("price_btc")
    }
  }

  test("Getting Mining Info"){
    get("/burst", Map("requestType" -> "getMiningInfo")){
      status should equal (200)
      body should include ("generationSignature")
    }
  }

  test("Getting pool statistics"){

  }

  test("Submitting a bad nonce"){
    val accId = "15240509513051186062"
    val nonce = "2"
    get("/burst", Map("requestType" -> "submitNonce",
      "accountId" -> accId, "nonce" -> nonce)){
      println(body)
      status should equal (200)
    }
  }

  test("Submitting a valid but not best nonce"){
    val accId = "15240509513051186062"
    val nonce = "889638458"
    get("/burst", Map("requestType" -> "submitNonce",
      "accountId" -> accId, "nonce" -> nonce)){
      status should equal (200)
    }
  }

  test("Banning a user"){
    Global.userManager ! banUser("1", LocalDateTime.now().plusSeconds(2))
    Global.userManager ! addUser("1", 2)
    (Global.userManager ? containsUser("1")) onSuccess {
      case Some(result) => {
        result.asInstanceOf[Boolean] should equal (false)
      }
    }
  }

  test("Unbanning a user"){
    Global.userManager ! banUser("1", LocalDateTime.now().minusSeconds(1))
    Global.userManager ! refreshUsers()
    Global.userManager ! addUser("1", 2)
    (Global.userManager ? containsUser("1")) onSuccess {
      case Some(result) => {
        result.asInstanceOf[Boolean] should equal (true)
      }
    }
  }

  test("Overwriting previous best nonce"){

  }

  test("100 user stress test"){

  }

  override def afterAll() {
    system.terminate()
    super.afterAll()
  }
}
