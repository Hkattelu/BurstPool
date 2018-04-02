package com.github.Chronox.pool.db

class Reward (
  var userId: Long,
  var blockId: Long,
  var currentPercent: Double,
  var historicalPercent: Double,
  var isPaid: Boolean,
  ) {
  def this() = this(0, 0, 0.0, 0.0, false)
}