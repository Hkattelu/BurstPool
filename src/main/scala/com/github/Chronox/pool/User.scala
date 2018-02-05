package com.github.Chronox.pool

import java.time.LocalDate
import java.time.format.DateTimeFormatter
import scala.concurrent.duration._

class User {
  var userName: String = null
  var miner_type: String = null
  var reported_TB: String = null
  var pendingBURST: Integer = null
  var payedBURST: Integer = null
  var lastSubmitTime: LocalDate = null
  var isBanned: Boolean = false
  var banTime: LocalDate = null
}