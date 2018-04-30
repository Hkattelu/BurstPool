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
  var testNodeURL = ""

  override def beforeAll(){
    super.beforeAll()
    Config.init()
    configureDb(true)
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
    Global.rewardAccumulator = 
      system.actorOf(Props[RewardAccumulator], name = "RewardAccumulator")
    Global.rewardPayout = 
      system.actorOf(Props[RewardPayout], name = "RewardPayout")
    Global.stateUpdater = 
      system.actorOf(Props(new StateUpdater(true)), name="StateUpdater")
    Global.dbWriter = 
      system.actorOf(Props[DatabaseWriter], name = "DatabaseWriter")
    Global.dbReader =
      system.actorOf(Props[DatabaseReader], name = "DatabaseReader")

    server.getConnectors.headOption match {
      case Some(conn) =>
        val networkConn = 
          conn.asInstanceOf[org.eclipse.jetty.server.NetworkConnector]
        val host = Option(networkConn.getHost).getOrElse("localhost")
        val port = networkConn.getLocalPort()
        testNodeURL = "http://" + host + ":" + port + "/test"
        require(port > 0, "The detected local port is < 1, that's not allowed")
        "http://%s:%d".format(host, port)
      case None =>
        sys.error("can't calculate base URL: no connector")
    }
    addServlet(classOf[PoolServlet], "/*")
    addServlet(classOf[BurstPriceServlet], "/getBurstPrice")
    addServlet(new MockBurstServlet(system), "/test")
    addServlet(new BurstServlet(system, system.actorOf(Props[SubmissionHandler],
      name="SubmissionHandler")), "/burst")

    Global.miningInfo = new Global.MiningInfo(
      "916b4758655bedb6690853edf33fc65a6b0e1b8f15b13f8615e053002cb06729", 
      "54752", "478972")
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

  test("Fail if submitting nonce without all parameters"){
    post("/burst", Map("requestType" -> "submitNonce")){
      status should equal(400)
    }
  }

  test("Fails on bad requestType"){
    post("/burst", Map("requestType" -> "somethingThatWontWork")){
      status should equal(400)
    }
  }

  test("Simple Shares to Reward calculation"){
    var weights = Map[User, Share]()
    var percents = Map[Long, BigDecimal]()

    // Add some shares, then calculate reward split of those shares
    val fraction = BigDecimal.valueOf(16)/BigDecimal.valueOf(15)
    for(i <- 1 to 4) {
      percents += (-i.toLong->(fraction/BigDecimal.valueOf(1 << i))
        .setScale(8, RoundingMode.HALF_EVEN))
      Global.shareManager ! addShare(new User(-i), -i, 0, 1 << i)
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
    for(i <- 1 to (Config.MIN_HEIGHT_DIFF + 10)) { 
      for(j <- 1 to 4) Global.shareManager ! addShare(users(j), i, 0, 1 << j)
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
    Global.rewardAccumulator ! addRewards(3, current, historic)
    val future = (Global.rewardAccumulator ? getUnpaidRewards())
      .mapTo[Map[Long, List[Reward]]]
    val calculated = Await.result(future, timeout.duration)
    var rewards = Map[Long, List[Reward]]()
    var rewardList = new ListBuffer[Reward]()
    for(i <- 1 to 4) 
      rewardList += (new Reward(i, 3, zero, quarter,false))
    rewardList += (new Reward(5, 3, one, zero, false))
    rewards += (3L->rewardList.toList)
    calculated.values.head.toSet should equal (rewards.values.head.toSet)
    Global.rewardAccumulator ! clearUnpaidRewards()
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

    val future = (Global.userManager ? containsActiveUser("1", 1))
      .mapTo[Boolean]
    Await.result(future, timeout.duration) should equal (false)
  }

  test("Unbanning a user"){
    Global.userManager ! resetUsers()

    // Unbanning a banned user, and then checking if they exist should work
    Global.userManager ! banUser("1", LocalDateTime.now().minusSeconds(1))
    Global.userManager ! refreshUsers()
    Global.userManager ? addUser("1", 2)

    val future = (Global.userManager ? containsActiveUser("1", 2))
      .mapTo[Boolean]
    Await.result(future, timeout.duration) should equal (true)
  }

  test("Rewards don't get lost on network error"){
    Global.rewardPayout ! Global.setSubmitURI(testNodeURL)
    Thread.sleep(1000)

    var current = Map[Long, BigDecimal]()
    var historic = Map[Long, BigDecimal]()
    for(i <- 2 to 5) historic += (i.toLong->quarter)
    current += (0.toLong->one)
    Global.rewardAccumulator ! addRewards(1, current, historic)// 2 causes error
    Thread.sleep(100)
    Global.rewardPayout ! PayoutRewards()
    Thread.sleep(1000)
    val future = (Global.rewardAccumulator ? getUnpaidRewards())
      .mapTo[Map[Long, List[Reward]]]
    val calculated = Await.result(future, timeout.duration)
    var rewards = Map[Long, List[Reward]]()
    var rewardList = new ListBuffer[Reward]()
    for(i <- 2 to 5) 
      rewardList += (new Reward(i, 1L, zero, quarter,false))
    rewardList += (new Reward(0, 1L, one, zero, false))
    rewards += (1L->rewardList.toList)
    calculated.values.head.toSet should equal (rewards.values.head.toSet)

    Global.rewardAccumulator ! clearUnpaidRewards()
  }

  test("Reward Transactions are successfully sent"){
    Global.rewardPayout ! Global.setSubmitURI(testNodeURL)

    var current = Map[Long, BigDecimal]()
    var historic = Map[Long, BigDecimal]()
    current += (1.toLong->one)
    Global.rewardAccumulator ! addRewards(2, current, historic)
    Thread.sleep(100)
    Global.rewardPayout ! PayoutRewards()
    Thread.sleep(1000)
    val future = (Global.rewardAccumulator ? getUnpaidRewards())
      .mapTo[Map[Long, List[Reward]]]
    val calculated = Await.result(future, timeout.duration)
    calculated should equal (Map[Long, List[Reward]]())
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
    deadline should equal (BigInteger.valueOf(273L))
  }

  test("Submitting a bad nonce"){
    val accId = "1" 
    val nonce = "1" // Random nonce, will probably be bad
    post("/burst", Map("requestType" -> "submitNonce",
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
    post("/burst", Map("requestType"->"submitNonce", "accountId"->accId, 
      "nonce"->nonce)) {
      body should include ("did not match calculated deadline")
      status should equal (500)
    }
  }

  test("Submitting a valid nonce adds Shares and sets best deadline"){
    // Use mock server to accept the nonce
    Global.deadlineSubmitter ! Global.setSubmitURI(testNodeURL)
    Global.shareManager ! dumpCurrentShares()

    // Constants from block at height 478972
    val accId = "7451808546734026404"
    val nonce = "151379672"
    Global.miningInfo = new Global.MiningInfo(
      "916b4758655bedb6690853edf33fc65a6b0e1b8f15b13f8615e053002cb06729", 
      "54752", "478972")
    post("/burst", Map("requestType"->"submitNonce", "accountId"->accId, 
      "nonce"->nonce)) {
      status should equal (200)
      Global.currentBestDeadline should equal (BigInteger.valueOf(273L))
      val future = (Global.shareManager ? getCurrentPercents())
        .mapTo[Map[Long, BigDecimal]]
      val shares = Await.result(future, timeout.duration)
      shares.keys should contain (new BigInteger(accId).longValue)
    }
  }

  override def afterAll() {
    system.terminate()
    closeDbConnection()
    super.afterAll()
  }
}