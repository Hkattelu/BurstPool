package com.github.Chronox.pool.db
import java.lang.Long
import java.math.BigInteger

class Reward (
  var userId: Long,
  var blockId: BigInteger,
  var currentPercent: BigDecimal,
  var historicalPercent: BigDecimal,
  var isPaid: Boolean,
) {
  def this() = this(0, BigInteger.valueOf(0), BigDecimal.valueOf(0), 
    BigDecimal.valueOf(0), false)
}