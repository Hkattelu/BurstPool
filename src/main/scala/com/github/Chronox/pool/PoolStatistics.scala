package com.github.Chronox.pool
import java.time.LocalDateTime

object PoolStatistics {
  var numValidNonces: Int = 0
  var numBadNonces: Int = 0
  var numBannedAddresses: Int = 0
  var numActiveUsers: Int = 0
  var numTotalUsers: Int = 0
  var netActiveTB: BigDecimal = 0.0
  var lastSubmitTime: LocalDateTime = null

  def resetCurrentStatistics() {
    numValidNonces = 0
    numBadNonces = 0
  }

  def incrementActiveUsers() {
    numActiveUsers += 1
    numTotalUsers += 1
  }

  def decrementActiveUsersBy(num: Int) {numActiveUsers -= num}
  def incrementBannedAddresses() {numBannedAddresses += 1}
  def decrementBannedAddressesBy(num: Int) {numBannedAddresses -= num}
  def updateSubmitTime(time: LocalDateTime) {lastSubmitTime = time}
  def resetActiveTB() {netActiveTB = 0}
  def addActiveTB(tb: BigDecimal) {netActiveTB += tb}
  def incrementValidNonces() {numValidNonces += 1}
  def incrementBadNonces() {numBadNonces += 1}
}