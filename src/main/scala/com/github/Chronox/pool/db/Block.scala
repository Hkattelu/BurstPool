package com.github.Chronox.pool.db
import org.squeryl.KeyedEntity

class Block (
  var id: Long,
  var height: Long,
  var nonce: Long,
  var blockReward: Long,
  var baseTarget: Long,
  var generatorId: Long,
  var generatorRS: String,
  var generationSig: String,
  var timestamp: String
  ) extends KeyedEntity[Long] {
  def this() = this(0, 0, 0, 0, 0, 0, null, null, null)
}