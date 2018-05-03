package com.github.Chronox.pool
import java.time.LocalDateTime
import java.sql.Timestamp
import java.util.concurrent.atomic.{AtomicInteger, AtomicLong}
import scala.collection.concurrent.TrieMap

case class statistics(validNonces: Int, badNonces: Int, bannedAddresses: Int,
  activeUsers: Int, totalUsers: Int, NQTEarned: Long, activeTB: BigDecimal, 
  minerCounts: Map[String, Int], serverUptime: Long, lastSubmitTime: Timestamp)

object PoolStatistics {
  val serverStartTime: Long = Timestamp.valueOf(LocalDateTime.now).getTime
  var numValidNonces: AtomicInteger = new AtomicInteger(0)
  var numBadNonces: AtomicInteger = new AtomicInteger(0)
  var numBannedAddresses: AtomicInteger = new AtomicInteger(0)
  var numActiveUsers: AtomicInteger = new AtomicInteger(0)
  var numTotalUsers: AtomicInteger = new AtomicInteger(0)
  var NQTEarned: AtomicLong = new AtomicLong(0L)
  var minerCounts: TrieMap[String, Int] = TrieMap[String, Int]()
  
  // Bigdecimal and Timestamp are thread-safe
  //var netActiveTB: BigDecimal = 0.0
  var lastSubmitTime: Timestamp = null

  def resetPoolStatistics() {
    resetCurrentStatistics()
    numActiveUsers.set(0)
    numTotalUsers.set(0)
    numBannedAddresses.set(0)
    NQTEarned.set(0L)
    //netActiveTB = 0.0
    lastSubmitTime = null
    minerCounts.clear()
  }

  def resetCurrentStatistics() {
    numValidNonces.set(0)
    numBadNonces.set(0)
  }

  def incrementActiveUsers() {
    numActiveUsers.incrementAndGet()
    numTotalUsers.incrementAndGet()
  }
  def decrementActiveUsersBy(num: Int) {numActiveUsers.addAndGet(-num)}
  def incrementBannedAddresses() {numBannedAddresses.incrementAndGet()}
  def decrementBannedAddressesBy(num: Int) {numBannedAddresses.addAndGet(-num)}
  def incrementNQTEarned(num: Long) {NQTEarned.addAndGet(num)}
  def updateSubmitTime(time: Timestamp) {lastSubmitTime = time}
  /*def resetActiveTB() {netActiveTB = 0}
  def addActiveTB(tb: BigDecimal) {netActiveTB += tb}*/
  def incrementValidNonces() {numValidNonces.incrementAndGet()}
  def incrementBadNonces() {numBadNonces.incrementAndGet()}
  def getServerUptime(): Long = { 
    Timestamp.valueOf(LocalDateTime.now).getTime - serverStartTime
  }
  def addMiner(miner_type: String) {
    minerCounts contains miner_type match {
      case true => minerCounts(miner_type)++
      case false => minerCounts += miner_type->1
    }
  }

  def get() {
    statistics(validNonces = numValidNonces.get, badNonces = numBadNonces.get,
      bannedAddresses = numBannedAddresses.get, lastSubmitTime = lastSubmitTime,
      activeUsers = numActiveUsers.get, serverUptime = getServerUptime(),
      totalUsers = numTotalUsers.get, activeTB = netActiveTB, 
      NQTEarned = NQTEarned.get, minerCounts = minerCounts.toMap)
  }
}