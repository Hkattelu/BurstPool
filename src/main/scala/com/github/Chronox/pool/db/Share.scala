package com.github.Chronox.pool.db

class Share(
  var burstValue: BigDecimal,
  var userId: Long,
  var submittedNonce: Long
  ) {
  def this() = this(0.0, 0, 0)
}