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
case class containsUser(ip_address: String)
case class addUser(ip_address: String, accountId: Long)
case class getUser(ip: String)
case class getActiveUsers()
case class banUser(ip_address: String, until: LocalDateTime)
case class refreshUsers()
case class updateSubmitTime(ip_address: String)
case class checkRewardRecipient(accId: String)
case class RewardRecipient(rewardRecipient: String)

class UserManager extends Actor with ActorLogging {

  import context.dispatcher
  final implicit val materializer: ActorMaterializer = 
    ActorMaterializer(ActorMaterializerSettings(context.system))

  val http = Http(context.system)
  implicit val formats = DefaultFormats
  var activeUsers: TrieMap[String, User] = TrieMap[String, User]()
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
    case containsUser(ip_address: String) => {
      sender ! ((activeUsers contains ip_address) && 
        !(bannedAddresses contains ip_address))
    }
    case addUser(ip_address: String, accountId: Long) => {
      // Add user with given IP and accountId if the IP wasn't banned
      var userToReturn: Option[User] = Option(null)
      if (!(bannedAddresses contains ip_address)){
        var newUser = new User
        newUser.isActive = true
        newUser.ip = ip_address
        newUser.id = accountId
        //newUser.reported_TB = 0.0
        newUser.lastSubmitTime = Timestamp.valueOf(LocalDateTime.now())
        newUser.lastSubmitHeight = 
          new BigInteger(Global.miningInfo.height).longValue
        activeUsers += (ip_address->newUser)
        Global.poolStatistics.incrementActiveUsers()
        //Global.poolStatistics.addActiveTB(newUser.reported_TB)
        userToReturn = Some(newUser)
      }
      sender ! userToReturn
    }
    case getUser(ip: String) => {
      sender ! activeUsers.getOrElse(ip, null)
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
    case updateSubmitTime(ip_address: String) => {
      // Update the last submit time of users who aren't banned
      if (!(bannedAddresses contains ip_address)){
        var userToUpdate = activeUsers(ip_address)
        userToUpdate.lastSubmitTime = Timestamp.valueOf(LocalDateTime.now())
        activeUsers(ip_address) = userToUpdate
        Global.poolStatistics.updateSubmitTime(userToUpdate.lastSubmitTime)
      } 
    }
    case checkRewardRecipient(accId: String) => {
      val s = sender
      val checkUrl = Config.NODE_ADDRESS + 
        "/burst?requestType=getRewardRecipient&account="+accId
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