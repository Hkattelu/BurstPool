package com.github.Chronox.pool

import org.scalatra.test.scalatest._
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

  addServlet(classOf[PoolServlet], "/*")
  addServlet(classOf[BurstPriceServlet], "/getBurstPrice")
  addServlet(classOf[BurstServlet], "/burst")

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
    }
  }

  test("Getting pool statistics"){

  }

  test("Submitting a bad nonce"){
    val accId = "15240509513051186062"
    val nonce = "2"
    get("/burst", Map("requestType" -> "submitNonce",
      "accountId" -> accId, "nonce" -> nonce)){
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

  test("Overwriting previous best nonce"){

  }

  test("100 user stress test"){

  }

  override def afterAll() {
    system.terminate()
    super.afterAll()
  }
}
