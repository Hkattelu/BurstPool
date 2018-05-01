package com.github.Chronox.pool.db
import org.squeryl.KeyedEntity

class PoolPayment (
  var id: Long,
  var nickName: Option[String],
  var pendingNQT: Long,
  var paidNQT: Long
  ) extends KeyedEntity[Long]{
  def this() = this(0, None, 0, 0)
}