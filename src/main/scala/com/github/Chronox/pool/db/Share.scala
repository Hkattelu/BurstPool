package com.github.Chronox.pool.db
import java.math.BigInteger

class Share(
  var userId: Long,
  var blockId: BigInteger,
  var nonce: Long,
  var deadline: BigInteger,
  var isPaid: Boolean
  ) {
  def this() = this(0, BigInteger.valueOf(0), 0, BigInteger.valueOf(0), false)
}