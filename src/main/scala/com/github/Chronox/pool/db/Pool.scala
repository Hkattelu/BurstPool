package com.github.Chronox.pool.db

class Pool (
  var burstToPay: BigDecimal,
  var burstCollected: BigDecimal
  ) {
  def this() = this(0.0, 0.0)
}