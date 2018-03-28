package com.github.Chronox.pool.db

class Share(
  var burstValue: BigDecimal,
  var userId: Long,
  var blockId: Long,
  var submittedNonce: Long,
  var isPending: Boolean,
  var isPaid: Boolean
  ) {
  def this() = this(0.0, 0, 0, 0, false, false)
}