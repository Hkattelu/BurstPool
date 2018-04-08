package com.github.Chronox.pool.db
import org.squeryl.PrimitiveTypeMode._
import org.squeryl.KeyedEntity
import org.squeryl.dsl.CompositeKey2

class Share (
  var userId: Long,
  var blockId: Long,
  var nonce: Long,
  var deadline: Option[Long] // Null deadline signifies a historic share
) extends KeyedEntity[CompositeKey2[Long, Long]] {
  def id = compositeKey(userId, blockId)
  def this() = this(0, 0, 0, null)
  def this(deadline: Long) = this(0, 0, 0, Some(deadline))
}