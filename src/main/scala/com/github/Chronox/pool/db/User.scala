package com.github.Chronox.pool.db

import java.time.LocalDateTime

class User (
  var id: String,
  var nickName: String,
  var isActive: Boolean,
  var lastSubmitTime: LocalDateTime,
  var pendingBURST: BigDecimal,
  var payedBURST: BigDecimal,
  var reported_TB: String,
  var miner_type: String,
  ) {
  def this() = this(null, null, false, null, 0.0, 0.0, null, null)
}