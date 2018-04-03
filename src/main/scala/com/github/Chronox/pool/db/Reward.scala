package com.github.Chronox.pool.db
import java.lang.Long
import java.math.BigInteger

class Reward (
  var userId: Long,
  var blockId: BigInteger,
  var currentPercent: Double,
  var historicalPercent: Double,
  var isPaid: Boolean,
) {
  def this() = this(0, BigInteger.valueOf(0), 0.0, 0.0, false)
}