package com.github.Chronox.pool
import java.sql.Timestamp
import java.util.concurrent.atomic.AtomicInteger

object PoolStatistics {
  var numValidNonces: AtomicInteger = new AtomicInteger(0)
  var numBadNonces: AtomicInteger = new AtomicInteger(0)
  var numBannedAddresses: AtomicInteger = new AtomicInteger(0)
  var numActiveUsers: AtomicInteger = new AtomicInteger(0)
  var numTotalUsers: AtomicInteger = new AtomicInteger(0)

  // Bigdecimal and localdatetime are thread-safe
  var netActiveTB: BigDecimal = 0.0
  var lastSubmitTime: Timestamp = null

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
  def updateSubmitTime(time: Timestamp) {lastSubmitTime = time}
  def resetActiveTB() {netActiveTB = 0}
  def addActiveTB(tb: BigDecimal) {netActiveTB += tb}
  def incrementValidNonces() {numValidNonces.incrementAndGet()}
  def incrementBadNonces() {numBadNonces.incrementAndGet()}
}