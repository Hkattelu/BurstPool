package com.github.Chronox.pool.db
import java.time.LocalDateTime

class User (
  var id: Long,
  var nickName: String,
  var isActive: Boolean,
  var lastSubmitTime: LocalDateTime,
  var pendingBURST: BigDecimal,
  var payedBURST: BigDecimal,
  var reported_TB: BigDecimal,
  var miner_type: String,
  ) {
  def this() = this(0L, null, false, null, 0.0, 0.0, null, null)
}