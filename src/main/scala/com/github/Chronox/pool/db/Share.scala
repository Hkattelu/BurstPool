package com.github.Chronox.pool.db
import java.lang.Long
import java.math.BigInteger

class Share(
  var userId: Long,
  var blockId: BigInteger,
  var nonce: Long,
  var deadline: Long,
  var isPaid: Boolean
) {
  def this() = this(0, BigInteger.valueOf(0), 0, 0L, false)
}