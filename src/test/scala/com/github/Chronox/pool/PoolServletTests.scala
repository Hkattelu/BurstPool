package com.github.Chronox.pool

import com.github.Chronox.pool._
import com.github.Chronox.pool.actors._
import com.github.Chronox.pool.servlets._
import com.github.Chronox.pool.db._
import akka.util.Timeout
import akka.pattern.ask
import scala.concurrent.{Await, ExecutionContext, Future}
import scala.util.{Failure, Success}
import scala.concurrent.duration._
import org.scalatra.test.scalatest._
import java.time.LocalDateTime
import org.scalatest.FunSuiteLike
import _root_.akka.actor.{Props, ActorSystem}
import javax.servlet.ServletContext

import java.lang.Long
import java.math.BigInteger
import scala.math.BigDecimal.RoundingMode

class PoolServletTests extends ScalatraSuite with FunSuiteLike{

  Config.init()
  val system = ActorSystem()
  Global.stateUpdater = system.actorOf(Props[StateUpdater])
  Global.burstPriceChecker = system.actorOf(Props[BurstPriceChecker])
  Global.miningInfoUpdater = system.actorOf(Props[MiningInfoUpdater])
  Global.deadlineSubmitter = system.actorOf(Props[DeadlineSubmitter])
  Global.deadlineChecker = system.actorOf(Props[DeadlineChecker])
  Global.userManager = system.actorOf(Props[UserManager])
  Global.shareManager = system.actorOf(Props[ShareManager])
  Global.rewardPayout = system.actorOf(Props[RewardPayout])
  
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

  test("Shabal works properly"){
    val accId: Long = 1
    val nonce: Long = 1
    val future = (Global.deadlineChecker ? nonceToDeadline(accId, nonce))
      .mapTo[BigInteger]
    val deadline = Await.result(future, timeout.duration)
    deadline.toString() should equal ("216907742700256")
  }

  test("Submitting a bad nonce"){
    val accId = "1" 
    val nonce = "1" // Random nonce, will probably be bad
    get("/burst", Map("requestType" -> "submitNonce",
      "accountId" -> accId, "nonce" -> nonce)){
      status should equal (500)
    }
  }

  test("Submitting a valid nonce"){
    // Hardcoded constants, should work
    val accId = "13606764479022549485"
    val nonce = "2801024039"
    Global.miningInfo = new MiningInfo( 
      "6acc1e02f47ef19ab24c2e0ca2af866e7a36afa9f18c6fe4a7300c125a862d5c",
      null, "59301", 476779L, null, null, null, null)
    get("/burst", Map("requestType" -> "submitNonce",
      "accountId" -> accId, "nonce" -> nonce)){
      status should equal (200)
    }
  }

  test("Banning a user"){
    Global.userManager ! banUser("1", LocalDateTime.now().plusSeconds(2))
    Global.userManager ! addUser("1", 2)
    val future = (Global.userManager ? containsUser("1")).mapTo[Boolean]
    Await.result(future, timeout.duration) should equal (false)
  }

  test("Unbanning a user"){
    Global.userManager ! banUser("1", LocalDateTime.now().minusSeconds(1))
    Global.userManager ! refreshUsers()
    Global.userManager ! addUser("1", 2)
    val future = (Global.userManager ? containsUser("1")).mapTo[Boolean]
    Await.result(future, timeout.duration) should equal (true)
  }

  test("Simple Shares to Reward calculation"){
    var weights = Map[User, Share]()
    var percents = Map[scala.Long, BigDecimal]()
    val fraction = BigDecimal.valueOf(16)/BigDecimal.valueOf(15)
    for(i <- 1 to 4) {
      percents += (i.toLong->(fraction/BigDecimal.valueOf(1 << i))
        .setScale(8, RoundingMode.HALF_EVEN))
      Global.shareManager ! addShare(new User(i), 
        BigInteger.valueOf(0), 0, 1 << i)
    }
    val future = (Global.shareManager ? getCurrentPercents()
      ).mapTo[Map[Long, BigDecimal]]
    Await.result(future, timeout.duration).toSet should equal (percents.toSet)
  }

  test("Historical Shares to Reward calculation (over the historical limit)"){
    var weights = Map[User, Share]()
    var percents = Map[scala.Long, BigDecimal]()
    var users = Map[Int, User]()
    val fraction = BigDecimal.valueOf(16)/BigDecimal.valueOf(15)
    for(i <- 1 to 4) users += (i->(new User(i)))
    for(i <- 1 to 4)
      percents += (i.toLong->(fraction/BigDecimal.valueOf(1 << i))
        .setScale(8, RoundingMode.HALF_EVEN))

    //Add a bunch of random shares that should get overwrriten
    Global.shareManager ! addShare(users(1), BigInteger.valueOf(0), 0, 2018)
    Global.shareManager ! addShare(users(2), BigInteger.valueOf(0), 0, 9001)
    Global.shareManager ! addShare(users(3), BigInteger.valueOf(0), 0, 1234)
    Global.shareManager ! addShare(users(4), BigInteger.valueOf(0), 0, 1337)
    Global.shareManager ! dumpCurrentShares()

    for(i <- 1 to (Config.MIN_HEIGHT_DIFF + 100)){ 
      for(j <- 1 to 4) Global.shareManager ! addShare(users(j), 
        BigInteger.valueOf(0), 0, 1 << j)
      Global.shareManager ! dumpCurrentShares()
    }
    val future = (Global.shareManager ? getAverageHistoricalPercents()
      ).mapTo[Map[Long, BigDecimal]]
    Await.result(future, timeout.duration).toSet should equal (percents.toSet)
  }

  test("Getting pool statistics"){

  }

  test("100 user stress test"){

  }

  override def afterAll() {
    system.terminate()
    super.afterAll()
  }
}
