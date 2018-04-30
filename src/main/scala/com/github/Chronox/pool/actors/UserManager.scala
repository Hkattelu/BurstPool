package com.github.Chronox.pool.actors
import com.github.Chronox.pool.{Global, Config}
import com.github.Chronox.pool.db.User

import akka.actor.{ Actor, ActorLogging }
import akka.http.scaladsl.Http
import akka.http.scaladsl.model._
import akka.util.ByteString
import akka.stream.{ ActorMaterializer, ActorMaterializerSettings }
import scala.util.{ Failure, Success }
import scala.concurrent.duration._
import scala.collection.concurrent.TrieMap
import net.liftweb.json._
import java.math.BigInteger
import java.time.LocalDateTime
import java.sql.Timestamp

case class resetUsers()
case class ipIsBanned(ip_address: String)
case class containsActiveUser(ip_address: String, accountId: Long)
case class getUser(accId: Long)
case class getActiveUsers()
case class banUser(ip_address: String, until: LocalDateTime)
case class refreshUsers()
case class updateSubmitTime(accId: Long)
case class checkRewardRecipient(accId: Long)
case class RewardRecipient(rewardRecipient: String)

// Overload constructors for addUser to make miner_type optional
case class addUser(
  ip_address: String, accountId: Long, miner_type: Option[String]) {
  def this(ip_address: String, accountId: Long) = 
    this(ip_address, accountId, None)
}
object addUser {
  def apply(ip_address: String, accountId: Long) = 
    new addUser(ip_address, accountId, None)
}

class UserManager extends Actor with ActorLogging {

  import context.dispatcher
  final implicit val materializer: ActorMaterializer = 
    ActorMaterializer(ActorMaterializerSettings(context.system))

  val http = Http(context.system)
  implicit val formats = DefaultFormats
  var activeUsers: TrieMap[Long, User] = TrieMap[Long, User]()
  var bannedAddresses = TrieMap[String, LocalDateTime]()
  var netActiveTB = 0.0

  override def preStart() {
    activeUsers = Global.poolDB.loadActiveUsers()
  }
  
  def receive() = {
    case resetUsers() => {
      activeUsers.clear()
      bannedAddresses.clear()
      Global.poolStatistics.resetPoolStatistics()
    }
    case ipIsBanned(ip_address: String) => {
      sender ! (bannedAddresses contains ip_address)
    }
    case containsActiveUser(ip_address: String, accountId: Long) => {
      sender ! (!(bannedAddresses contains ip_address) &&
        (activeUsers contains accountId))
    }
    case addUser(ip_address: String, accountId: Long, 
      miner_type: Option[String]) => {
      // Add user with given IP and accountId if the IP wasn't banned
      var userToReturn: Option[User] = Option(null)
      if (!(bannedAddresses contains ip_address)){
        var newUser = new User()
        newUser.isActive = true
        newUser.ip = ip_address
        newUser.id = accountId
        newUser.lastSubmitTime = Timestamp.valueOf(LocalDateTime.now())
        newUser.lastSubmitHeight = 
          new BigInteger(Global.miningInfo.height).longValue
        newUser.miner_type = miner_type
        //newUser.reported_TB = 0.0
        activeUsers += (accountId->newUser)
        //Global.poolStatistics.addActiveTB(newUser.reported_TB)
        Global.poolStatistics.incrementActiveUsers()
        Global.DBWriter ! writeFunction(
          () => Global.poolDB.addUser(newUser))
        userToReturn = Some(newUser)
      }
      sender ! userToReturn
    }
    case getUser(accId: Long) => {
      var user: User = activeUsers.getOrElse(accId, null)
      if (user == null) {
        user = Global.poolDB.getInactiveUser(accId)
        if (user != null) {
          activeUsers += (accId->user)
          user.isActive = true
          user.lastSubmitTime = Timestamp.valueOf(LocalDateTime.now())
          user.lastSubmitHeight = 
            new BigInteger(Global.miningInfo.height).longValue
          Global.poolStatistics.incrementActiveUsers()
          Global.DBWriter ! writeFunction(
            () => Global.poolDB.updateUsers(List[User](user)))
          sender ! Some(user)
        } else {
          sender ! None
        }
      } else {
        sender ! Some(user)
      }
    }
    case banUser(ip_address: String, until: LocalDateTime) => {
      bannedAddresses += (ip_address->until)
      Global.poolStatistics.incrementBannedAddresses()
    }
    case refreshUsers() => {
      // Unban users who have been banned for enough time
      var prevNum: Int = bannedAddresses.size
      bannedAddresses.retain((k,v) => v isAfter LocalDateTime.now())
      Global.poolStatistics.decrementBannedAddressesBy(
        prevNum - bannedAddresses.size)

      // Clear active users who haven't submitted a deadline in the specified
      // number of blocks
      prevNum = activeUsers.size
      val inActiveUsers = activeUsers.filter((t) => {
        t._2.lastSubmitHeight <= (
          Global.miningInfo.height.toLong - Config.MIN_HEIGHT_DIFF)})
      Global.DBWriter ! writeFunction(
        () => Global.poolDB.updateUsers(inActiveUsers.map{ case (k,v) => {
        v.isActive = false; (k, v)}}.values.toList))

      activeUsers.retain((k,v) => {
        v.lastSubmitHeight > (
          Global.miningInfo.height.toLong - Config.MIN_HEIGHT_DIFF)})
      Global.poolStatistics.decrementActiveUsersBy(prevNum - activeUsers.size)

      // Recalculate the total active network TB if users went inactive
      if (activeUsers.size != prevNum){
        Global.poolStatistics.resetActiveTB()
        for ((k,v) <- activeUsers) {
          //Global.poolStatistics.addActiveTB(v.reported_TB)
        }
      }
    }
    case updateSubmitTime(accId: Long) => {
      // Update the last submit time of users who aren't banned
      var userToUpdate = activeUsers(accId)
      userToUpdate.lastSubmitTime = Timestamp.valueOf(LocalDateTime.now())
      activeUsers(accId) = userToUpdate
      Global.poolStatistics.updateSubmitTime(userToUpdate.lastSubmitTime)
    }
    case checkRewardRecipient(accId: Long) => {
      val s = sender
      val checkUrl = Config.NODE_ADDRESS + 
        "/burst?requestType=getRewardRecipient&account="+accId.toString
      http.singleRequest(HttpRequest(uri = checkUrl)) onComplete {
        case Success(r: HttpResponse) => {
          r.entity.dataBytes.runFold(ByteString(""))(_ ++ _) foreach { body => 
            if (body.utf8String contains "error") s ! false
            else s ! (parse(body.utf8String).extract[RewardRecipient]
              .rewardRecipient == Config.ACCOUNT_ID)
          }
        }
        case Failure(e) => s ! false
      }
    }
  }
}