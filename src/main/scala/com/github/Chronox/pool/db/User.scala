package com.github.Chronox.pool.db
import org.squeryl.KeyedEntity
import java.time.LocalDateTime
import java.math.BigInteger

class User (
  var id: Long,
  var lastSubmitHeight: Long,
  var nickName: Option[String],
  var isActive: Boolean,
  var lastSubmitTime: LocalDateTime,
  var pendingNQT: BigInteger,
  var payedNQT: BigInteger,
  var reported_TB: Option[BigDecimal],
  var miner_type: Option[String],
) extends KeyedEntity[Long] {
  def this() = this(0, 0, null, false, null, 
    BigInteger.valueOf(0), BigInteger.valueOf(0), null, null)
  def this(id: Long) = this(id, 0, null, false, null, 
    BigInteger.valueOf(0), BigInteger.valueOf(0), null, null)
}