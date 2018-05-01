package com.github.Chronox.pool.db
import org.squeryl.KeyedEntity

class PoolPayment (
  var id: Long,
  var pendingNQT: Long,
  var paidNQT: Long
  ) extends KeyedEntity[Long]{
  def this() = this(0, 0, 0)
}