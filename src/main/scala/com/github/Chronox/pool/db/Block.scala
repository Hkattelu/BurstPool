package com.github.Chronox.pool.db

import java.time.LocalDate

class Block (
  var rewardId: Long,
  var height: Long,
  var deadline: Long,
  var difficulty: Long,
  var baseTarget: Long,
  var generator: String,
  var generationSig: String,
  var blockSig: String,
  var rewardAssignment: String,
  var timeSubmitted: LocalDate
  ) {
  def this() = this(0, 0, 0, 0, 0, null, null, null, null, null)
}