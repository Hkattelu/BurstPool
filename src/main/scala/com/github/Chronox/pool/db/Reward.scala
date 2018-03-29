package com.github.Chronox.pool.db

class Reward (
  var blockId: Long,
  var userId: Long
  ) {
  def this() = this(0, 0)
}