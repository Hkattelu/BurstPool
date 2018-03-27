package com.github.Chronox.pool

import com.github.Chronox.pool.db.User

import scala.concurrent.duration._
import java.time.LocalDateTime

object UserManager {

  var activeUsers = scala.collection.mutable.Map[String, User]()
  var bannedAddresses = scala.collection.mutable.Map[String, LocalDateTime]()
  var netActiveTB = 0.0

  def containsUser(ip_address: String): Boolean = {
    return (activeUsers contains ip_address) && 
      !(bannedAddresses contains ip_address)
  }

  def addUser(ip_address: String, accountId: Long): Boolean = {
    // Don't add user's with banned IP's
    if (bannedAddresses contains ip_address) return false
    var newUser = new User
    newUser.isActive = true
    newUser.id = accountId
    newUser.reported_TB = 0.0
    newUser.lastSubmitTime = LocalDateTime.now()
    activeUsers += (ip_address->newUser)
    Global.poolStatistics.incrementActiveUsers()
    Global.poolStatistics.addActiveTB(newUser.reported_TB)
    return true
  }

  def getUser(ip: String): User = {
    activeUsers.getOrElse(ip, null)
  }

  def banUser(ip_address: String, until: LocalDateTime) = {
    bannedAddresses += (ip_address->until)
    Global.poolStatistics.incrementBannedAddresses()
  }

  def refreshUsers() = {
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

  def updateSubmitTime(ip_address: String): Boolean = {
    if (bannedAddresses contains ip_address) false
    var userToUpdate = activeUsers(ip_address)
    userToUpdate.lastSubmitTime = LocalDateTime.now()
    activeUsers(ip_address) = userToUpdate
    Global.poolStatistics.updateSubmitTime(userToUpdate.lastSubmitTime)
    return true
  }
}