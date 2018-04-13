package com.github.Chronox.pool.db
import org.squeryl.KeyedEntity
import java.sql.Timestamp

class Block (
  var id: Long,
  var height: Long,
  var deadline: Option[Long],
  var rewardNQT: Long,
  var difficulty: Long,
  var baseTarget: Long,
  var generator: Option[Long],
  var generationSig: String,
  var timeSubmitted: Option[Timestamp]
  ) extends KeyedEntity[Long] {
  def this() = this(0, 0, null, 0, 0, 0, null, "", null)
}