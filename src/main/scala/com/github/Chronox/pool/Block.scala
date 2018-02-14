package com.github.Chronox.pool

import java.time.LocalDate

class Block (
  var height: Long,
  var deadline: Long,
  var difficulty: Long,
  var baseTarget: Long,
  var generator: String,
  var generationSig: String,
  var blockSig: String,
  var rewardAssignment: String
  var timeSubmitted: LocalDate,
  var rewardId: Long
  ) {
  def this() = this(0, 0, 0, 0, null, null, null, null)
}