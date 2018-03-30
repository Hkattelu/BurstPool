package com.github.Chronox.pool.actors

import com.github.Chronox.pool.Global
import com.github.Chronox.pool.Config
import com.github.Chronox.pool.db.User

import akka.actor.{ Actor, ActorLogging }
import akka.http.scaladsl.model._
import akka.stream.{ ActorMaterializer, ActorMaterializerSettings }
import scala.util.{ Failure, Success }
import scala.concurrent.duration._
import scala.collection.concurrent.TrieMap
import java.time.LocalDateTime

case class containsUser(ip_address: String)
case class addUser(ip_address: String, accountId: Long)
case class getUser(ip: String)
case class banUser(ip_address: String, until: LocalDateTime)
case class refreshUsers()
case class updateSubmitTime(ip_address: String)

class UserManager extends Actor with ActorLogging {

  var activeUsers = TrieMap[String, User]()
  var bannedAddresses = TrieMap[String, LocalDateTime]()
  var netActiveTB = 0.0

  def receive() = {
    case containsUser(ip_address: String) => {
      sender ! ((activeUsers contains ip_address) && 
        !(bannedAddresses contains ip_address))
    }
    case addUser(ip_address: String, accountId: Long) => {
      if (!(bannedAddresses contains ip_address)){
        var newUser = new User
        newUser.isActive = true
        newUser.id = accountId
        newUser.reported_TB = 0.0
        newUser.lastSubmitTime = LocalDateTime.now()
        activeUsers += (ip_address->newUser)
        Global.poolStatistics.incrementActiveUsers()
        Global.poolStatistics.addActiveTB(newUser.reported_TB)
      }
    }
    case getUser(ip: String) => {
      sender ! activeUsers.getOrElse(ip, null)
    }
    case banUser(ip_address: String, until: LocalDateTime) => {
      bannedAddresses += (ip_address->until)
      Global.poolStatistics.incrementBannedAddresses()
    }
    case refreshUsers() => {
      var prevNum: Int = bannedAddresses.size
      bannedAddresses.retain((k,v) => v isAfter LocalDateTime.now())
      Global.poolStatistics.decrementBannedAddressesBy(
        bannedAddresses.size - prevNum)

      prevNum = activeUsers.size
      activeUsers.retain((k,v) => {
        v.lastSubmitTime isBefore (
          LocalDateTime.now().minusDays(Config.DAYS_UNTIL_INACTIVE))
        })
      Global.poolStatistics.decrementActiveUsersBy(activeUsers.size - prevNum)

      if (activeUsers.size != prevNum){
        Global.poolStatistics.resetActiveTB()
        for ((k,v) <- activeUsers) {
          Global.poolStatistics.addActiveTB(v.reported_TB)
        }
      }
    }
    case updateSubmitTime(ip_address: String) => {
      if (!(bannedAddresses contains ip_address)){
        var userToUpdate = activeUsers(ip_address)
        userToUpdate.lastSubmitTime = LocalDateTime.now()
        activeUsers(ip_address) = userToUpdate
        Global.poolStatistics.updateSubmitTime(userToUpdate.lastSubmitTime)
      } 
    }
  }
}