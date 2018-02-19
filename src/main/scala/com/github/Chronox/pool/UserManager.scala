package com.github.Chronox.pool

import scala.concurrent.duration._
import java.time.LocalDate

object UserManager {

  var activeUsers = scala.collection.mutable.Map[String, User]()
  var bannedAddresses = scala.collection.mutable.Map[String, LocalDate]()

  def containsUser(ip_address: String): Boolean = {
    activeUsers contains ip_address
  }

  def addUser(ip_address: String, accountId: String): Boolean = {
    // Don't add user's with banned IP's
    if (bannedAddresses contains ip_address) false
    var newUser = new User
    newUser.isActive = true
    newUser.id = accountId
    newUser.lastSubmitTime = LocalDate.now()
    activeUsers += (ip_address->newUser)
    true
  }

  def banUser(ip_address: String, until: LocalDate) = {
    bannedAddresses += (ip_address->until)
  }

  def refreshUsers() = {
    bannedAddresses.retain((k,v) => v isAfter LocalDate.now())
  }

  def updateSubmitTime(ip_address: String): Boolean = {
    if (bannedAddresses contains ip_address) false
    var userToUpdate = activeUsers(ip_address)
    userToUpdate.lastSubmitTime = LocalDate.now()
    activeUsers(ip_address) = userToUpdate
    true
  }
}