package com.github.Chronox.pool.actors
import com.github.Chronox.pool.Global

import akka.actor.{Actor, ActorLogging}
import akka.stream.{ActorMaterializer, ActorMaterializerSettings}
import hash.{Shabal256, Convert, MiningPlot}
import java.math.BigInteger
import java.nio.ByteBuffer

case class nonceToDeadline(accountId: Long, nonce: Long)

class DeadlineChecker extends Actor with ActorLogging {

  val HASH_SIZE = 32
  val HASHES_PER_SCOOP = 2
  val SCOOP_SIZE = HASHES_PER_SCOOP * HASH_SIZE
  val SCOOPS_PER_PLOT = 4096 // original 1MB/plot = 16384
  val PLOT_SIZE = SCOOPS_PER_PLOT * SCOOP_SIZE
  val HASH_CAP = 4096
  val md = new Shabal256()
  
  def receive() = {
    case nonceToDeadline(accountId: Long, nonce: Long) => {
      sender ! calculateDeadline(accountId, nonce)
    }
  }

  var scoopNum = 0
  var scoopNumHeight = 0L

  def calculateDeadline(accId: Long, nonce: Long): BigInteger = {
    val plot = new MiningPlot(accId, nonce) // Use a java implementation
    if(Global.miningInfo.height.toLong != scoopNumHeight)
      calculateScoopNum
    md.reset()
    md.update(Convert.parseHexString(Global.miningInfo.generationSignature))
    plot.hashScoop(md, scoopNum)
    val hash = md.digest()
    val hit = new BigInteger(1, Array[Byte](hash(7), 
      hash(6), hash(5), hash(4), hash(3), hash(2), hash(1), hash(0)))
    return hit.divide(BigInteger.valueOf(Global.miningInfo.baseTarget.toLong))
  }
  
  def calculateScoopNum {
    val scoopBuffer = ByteBuffer.allocate(32 + 8)
    scoopBuffer.put(
      Convert.parseHexString(Global.miningInfo.generationSignature))
    scoopBuffer.putLong(Global.miningInfo.height.toLong)
    md.reset()
    md.update(scoopBuffer.array())
    val scoopHash = new BigInteger(1, md.digest())
    scoopNum = scoopHash.mod(BigInteger.valueOf(SCOOPS_PER_PLOT)).intValue()
    scoopNumHeight = Global.miningInfo.height.toLong
  }
}