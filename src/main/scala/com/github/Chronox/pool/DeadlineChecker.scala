package com.github.Chronox.pool

import java.math.BigInteger
import java.nio.ByteBuffer

object DeadlineChecker {

  var currentScoopNum: Int = -1
  val shabal = new Shabal256()

  def verifyNonce(accountId: String, nonce: String): Boolean = {
    shabal.update(genSigBytes)
    val heightBytes = ByteBuffer.allocate(Long.SIZE / Byte.SIZE).putLong(
      Global.miningInfo.height).array()
    shabal.update(heightBytes)
  }

  def getScoopNum(): Int = {
    val seedBuffer = ByteBuffer.allocate(40) // 32 byte gensig + 8byte height
    val genSigBytes = new BigInteger(
      Global.miningInfo.generationSignature, 16).toByteArray
    seedBuffer.put(genSigBytes)
    seedBuffer.putLong(Global.miningInfo.height)
    shabal.reset()
    shabal.update(seedBuffer.array())
    val generationHash = new BigInteger(1,shabal.digest())
    return generationHash.mod(Global.SCOOPS_PER_PLOT).intValue()
  }
}