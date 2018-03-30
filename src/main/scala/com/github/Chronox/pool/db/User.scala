package com.github.Chronox.pool.db
import java.time.LocalDateTime
import java.math.BigInteger

class User (
  var id: Long,
  var nickName: String,
  var isActive: Boolean,
  var lastSubmitTime: LocalDateTime,
  var pendingNQT: BigInteger,
  var payedNQT: BigInteger,
  var reported_TB: BigDecimal,
  var miner_type: String,
  ) {
  def this() = this(0L, null, false, null, 
    BigInteger.valueOf(0L), BigInteger.valueOf(0L), null, null)
}