package com.github.Chronox.pool

import db.User

import scala.concurrent.duration._
import java.time.LocalDateTime

object UserManager {

  var activeUsers = scala.collection.mutable.Map[String, User]()
  var bannedAddresses = scala.collection.mutable.Map[String, LocalDateTime]()

  def containsUser(ip_address: String): Boolean = {
    activeUsers contains ip_address
  }

  def addUser(ip_address: String, accountId: String): Boolean = {
    // Don't add user's with banned IP's
    if (bannedAddresses contains ip_address) return false
    var newUser = new User
    newUser.isActive = true
    newUser.id = accountId
    newUser.lastSubmitTime = LocalDateTime.now()
    activeUsers += (ip_address->newUser)
    return true
  }

  def banUser(ip_address: String, until: LocalDateTime) = {
    bannedAddresses += (ip_address->until)
    println(LocalDateTime.now().toString())
    println(ip_address + ":" + until.toString())
  }

  def refreshUsers() = {
    bannedAddresses.retain((k,v) => v isAfter LocalDateTime.now())
  }

  def updateSubmitTime(ip_address: String): Boolean = {
    if (bannedAddresses contains ip_address) return false
    var userToUpdate = activeUsers(ip_address)
    userToUpdate.lastSubmitTime = LocalDateTime.now()
    activeUsers(ip_address) = userToUpdate
    return true
  }
}