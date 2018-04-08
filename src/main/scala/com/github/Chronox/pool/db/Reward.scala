package com.github.Chronox.pool.db
import org.squeryl.PrimitiveTypeMode._
import org.squeryl.KeyedEntity
import org.squeryl.dsl.CompositeKey2

class Reward (
  var userId: Long,
  var blockId: Long,
  var currentPercent: BigDecimal,
  var historicalPercent: BigDecimal,
  var isPaid: Boolean,
) extends KeyedEntity[CompositeKey2[Long, Long]] {
  def id = compositeKey(userId, blockId)
  def this() = this(0, 0, BigDecimal.valueOf(0), 
    BigDecimal.valueOf(0), false)
}