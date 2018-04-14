package com.github.Chronox.pool

import com.github.Chronox.pool._
import com.github.Chronox.pool.actors._
import com.github.Chronox.pool.servlets._
import com.github.Chronox.pool.db._
import akka.util.Timeout
import akka.pattern.ask
import scala.collection.mutable.ListBuffer
import scala.collection.concurrent.TrieMap
import scala.concurrent.{Await, ExecutionContext, Future}
import scala.util.{Failure, Success}
import scala.concurrent.duration._
import org.scalatra.test.scalatest._
import org.scalatest.FunSuiteLike
import org.scalatra._
import _root_.akka.actor.{Props, ActorSystem}
import scalaj.http.Http
import net.liftweb.json._
import java.time.LocalDateTime
import java.math.BigInteger
import scala.math.BigDecimal.RoundingMode

class PoolServletTests extends ScalatraSuite with FunSuiteLike 
with DatabaseInit {

  val system = ActorSystem()
  implicit val formats = DefaultFormats
  protected implicit def executor: ExecutionContext = system.dispatcher
  protected implicit val timeout: Timeout = 5 seconds

  val zero = BigDecimal.valueOf(0)
  val quarter = BigDecimal.valueOf(0.25)
  val one = BigDecimal.valueOf(1)
  val testNodeURL = "https://127.0.0.1:8125/burst"

  override def beforeAll(){
    Config.init()
    //configureDb()
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
      system.actorOf(Props[StateUpdater], name="StateUpdater")
    
    addServlet(classOf[PoolServlet], "/*")
    addServlet(classOf[BurstPriceServlet], "/getBurstPrice")
    addServlet(new MockBurstServlet(system), "/test")
    addServlet(new BurstServlet(system), "/burst")
    //system.stop(Global.miningInfoUpdater)
    super.beforeAll()
  }

  test("All servlets up and running"){
    get("/"){
      status should equal (200)
    }

    get("/getBurstPrice"){
      status should equal (200)
    }

    get("/burst"){
      status should equal (400)
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
      body should include ("baseTarget")
      body should include ("height")
    }
  }

  test("Shabal works properly"){
    // Constants from block at height 478972
    val accId: Long = new BigInteger("7451808546734026404").longValue()
    val nonce: Long = new BigInteger("151379672").longValue()
    Global.miningInfo = new Global.MiningInfo(
      "916b4758655bedb6690853edf33fc65a6b0e1b8f15b13f8615e053002cb06729", 
      "54752", "478972")
    val future = (Global.deadlineChecker ? nonceToDeadline(accId, nonce))
      .mapTo[BigInteger]
    val deadline = Await.result(future, timeout.duration)
    deadline.toString() should equal ("273")
  }

  test("Simple Shares to Reward calculation"){
    var weights = Map[User, Share]()
    var percents = Map[Long, BigDecimal]()

    // Add some shares, then calculate reward split of those shares
    val fraction = BigDecimal.valueOf(16)/BigDecimal.valueOf(15)
    for(i <- 1 to 4) {
      percents += (i.toLong->(fraction/BigDecimal.valueOf(1 << i))
        .setScale(8, RoundingMode.HALF_EVEN))
      Global.shareManager ! addShare(new User(i), 0, 0, 1 << i)
    }
    val future = (Global.shareManager ? getCurrentPercents()
      ).mapTo[Map[Long, BigDecimal]]
    Await.result(future, timeout.duration).toSet should equal (percents.toSet)
  }

  test("Historical Shares to Reward calculation (over the historical limit)"){
    var weights = Map[User, Share]()
    var percents = Map[Long, BigDecimal]()
    var users = Map[Int, User]()
    val fraction = BigDecimal.valueOf(16)/BigDecimal.valueOf(15)
    for(i <- 1 to 4) users += (i->(new User(i)))
    for(i <- 1 to 4)
      percents += (i.toLong->(fraction/BigDecimal.valueOf(1 << i))
        .setScale(8, RoundingMode.HALF_EVEN))

    // Add a bunch of random shares
    Global.shareManager ! addShare(users(1), 0, 0, 2018)
    Global.shareManager ! addShare(users(2), 0, 0, 9001)
    Global.shareManager ! addShare(users(3), 0, 0, 1234)
    Global.shareManager ! addShare(users(4), 0, 0, 1337)
    Global.shareManager ! dumpCurrentShares()

    // Save 100+ shares to historic shares, they'll overwrite the random shares
    for(i <- 1 to (Config.MIN_HEIGHT_DIFF + 100)){ 
      for(j <- 1 to 4) Global.shareManager ! addShare(users(j), 0, 0, 1 << j)
        Global.shareManager ! dumpCurrentShares()
    }
    val future = (Global.shareManager ? getAverageHistoricalPercents()
      ).mapTo[Map[Long, BigDecimal]]
    Await.result(future, timeout.duration).toSet should equal (percents.toSet)
  }

  test("Rewards accumulate properly"){
    var current = Map[Long, BigDecimal]()
    var historic = Map[Long, BigDecimal]()
    for(i <- 1 to 4) historic += (i.toLong->quarter)
    current += (5.toLong->one)
    Global.rewardPayout ! addRewards(1, current, historic)
    val future = (Global.rewardPayout ? getRewards())
      .mapTo[Map[Long, List[Reward]]]
    val calculated = Await.result(future, timeout.duration)
    var rewards = Map[Long, List[Reward]]()
    var rewardList = new ListBuffer[Reward]()
    for(i <- 1 to 4) 
      rewardList += (new Reward(i, 1, zero, quarter,false))
    rewardList += (new Reward(5, 1, one, zero, false))
    rewards += (1L->rewardList.toList)
    calculated.values.head.toSet should equal (rewards.values.head.toSet)
    Global.rewardPayout ! clearRewards()
  }

  test("Rewards don't get lost on network error"){
  }

  test("Reward Transactions are successfully sent"){
  }

  test("Rewards are queue'd when mining information changes"){
  }

  test("Best deadline is overwritten on better deadline"){
  }

  test("Adding Users pool statistics"){
    Global.userManager ! resetUsers()

    // Adding 5 users should set activeUsers to 5
    Global.poolStatistics.numActiveUsers.get() should equal (0)
    Global.poolStatistics.numTotalUsers.get() should equal (0)
    for(i <- 1 to 5) Global.userManager ? addUser(i.toString(), i.toLong)
    Thread.sleep(50)
    Global.poolStatistics.numTotalUsers.get() should equal (5)
    Global.poolStatistics.numActiveUsers.get() should equal (5)
  }

  test("Banning Users pool statistics"){
    Global.userManager ! resetUsers()

    // Banning a user should increment bannedAddresses
    Global.userManager ! banUser("1", LocalDateTime.now().minusSeconds(1))

    Thread.sleep(50)
    Global.poolStatistics.numBannedAddresses.get() should equal (1)
    Global.poolStatistics.numActiveUsers.get() should equal (0)
    Global.poolStatistics.numTotalUsers.get() should equal (0)
    Global.userManager ! refreshUsers()

    Thread.sleep(50)
    Global.poolStatistics.numBannedAddresses.get() should equal (0)
  }

  test("Banning a user"){
    Global.userManager ! resetUsers()

    // Should not be able to add a user who's IP is banned
    Global.userManager ! banUser("1", LocalDateTime.now().plusSeconds(5))
    Global.userManager ? addUser("1", 1)

    val future = (Global.userManager ? containsUser("1")).mapTo[Boolean]
    Await.result(future, timeout.duration) should equal (false)
  }

  test("Unbanning a user"){
    Global.userManager ! resetUsers()

    // Unbanning a banned user, and then checking if they exist should work
    Global.userManager ! banUser("1", LocalDateTime.now().minusSeconds(1))
    Global.userManager ! refreshUsers()
    Global.userManager ? addUser("1", 2)

    val future = (Global.userManager ? containsUser("1")).mapTo[Boolean]
    Await.result(future, timeout.duration) should equal (true)
  }

  test("Submitting a bad nonce"){
    val accId = "1" 
    val nonce = "1" // Random nonce, will probably be bad
    get("/burst", Map("requestType" -> "submitNonce",
      "accountId" -> accId, "nonce" -> nonce)){
      status should equal (500)
    }
  }

  test("Submitting a valid nonce for an old block"){
    Global.userManager ! resetUsers()

    // Constants from block at height 478972
    val accId = "7451808546734026404"
    val nonce = "151379672"
    Global.miningInfo = new Global.MiningInfo(
      "916b4758655bedb6690853edf33fc65a6b0e1b8f15b13f8615e053002cb06729", 
      "54752", "478972")
    get("/burst", Map("requestType"->"submitNonce", "accountId"->accId, 
      "nonce"->nonce)) {
      body should include ("did not match calculated deadline")
      status should equal (500)
    }
  }

  test("Submitting a valid nonce adds Shares and sets best deadline"){
    // Constants from block at height 478972
    val accId = "7451808546734026404"
    val nonce = "151379672"
    Global.miningInfo = new Global.MiningInfo(
      "916b4758655bedb6690853edf33fc65a6b0e1b8f15b13f8615e053002cb06729", 
      "54752", "478972")
    get("/burst", Map("requestType"->"submitNonce", "accountId"->accId, 
      "nonce"->nonce)) {
      body should include ("did not match calculated deadline")
      status should equal (500)
    }
  }

  override def afterAll() {
    system.terminate()
    closeDbConnection()
    super.afterAll()
  }
}
