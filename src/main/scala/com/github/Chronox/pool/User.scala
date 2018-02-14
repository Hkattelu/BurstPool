package com.github.Chronox.pool

import java.time.LocalDate

class User (
  var id: String,
  var nickName: String,
  var isActive: Boolean,
  var lastSubmitTime: LocalDate,
  var pendingBURST: BigDecimal,
  var payedBURST: BigDecimal,
  var reported_TB: String,
  var miner_type: String,
  var isBanned: Boolean,
  var unbanAtTime: LocalDate
  ) {
  def this() = this(null, null, false, null, 0.0, 0.0, null, null, false, null)
}