package com.github.Chronox.pool.db
import org.squeryl.KeyedEntity
import java.time.LocalDateTime

class User (
  var id: Long,
  var ip: String,
  var lastSubmitHeight: Long,
  var nickName: Option[String],
  var isActive: Boolean,
  var lastSubmitTime: LocalDateTime,
  var pendingNQT: Long,
  var payedNQT: Long,
  var reported_TB: Option[BigDecimal],
  var miner_type: Option[String],
) extends KeyedEntity[Long] {
  def this() = this(0, null, 0, null, false, null, 0L, 0L, null, null)
  def this(id: Long) = this(id, null, 0, null, false, null, 0L, 0L, null, null)
}