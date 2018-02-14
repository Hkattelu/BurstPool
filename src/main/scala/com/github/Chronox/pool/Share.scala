package com.github.Chronox.pool

class Share(
  var burstValue: BigDecimal,
  var userId: Long,
  var submittedNonce: Long
  ) {
  def this() = this(0.0, 0, 0)
}