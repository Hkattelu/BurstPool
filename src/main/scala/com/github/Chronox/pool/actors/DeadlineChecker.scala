package com.github.Chronox.pool.actors

import com.github.Chronox.pool.Global

import akka.actor.{ Actor, ActorLogging }
import akka.http.scaladsl.model._
import akka.stream.{ ActorMaterializer, ActorMaterializerSettings }
import scala.util.{ Failure, Success }
import scala.concurrent.duration._

import hash.Shabal256
import java.math.BigInteger
import java.nio.ByteBuffer

case class nonceToDeadline(accountId: Long, nonce: Long)

class DeadlineChecker extends Actor with ActorLogging {

  val HASH_SIZE = 32;
  val HASHES_PER_SCOOP = 2;
  val SCOOP_SIZE = HASHES_PER_SCOOP * HASH_SIZE;
  val SCOOPS_PER_PLOT = 4096; // original 1MB/plot = 16384
  val PLOT_SIZE = SCOOPS_PER_PLOT * SCOOP_SIZE;
  val HASH_CAP = 4096;
  val md = new Shabal256()
  
  def receive() = {
    case nonceToDeadline(accountId: Long, nonce: Long) => {
      sender ! calculateDeadline(accountId, nonce)
    }
  }

  var scoopNum = 0
  var scoopNumHeight = 0L

  def calculateDeadline(address: Long, nonce: Long): BigInteger = {
    val plot = new MiningPlot(address, nonce)
    if(Global.miningInfo.height.toLong != scoopNumHeight) {
      calculateScoopNum
    }
    md.reset()
    val seedBuffer = ByteBuffer.allocate(40)
    var genSigBytes = new BigInteger(
        Global.miningInfo.generationSignature, 16).toByteArray
    seedBuffer.put(genSigBytes)
    seedBuffer.putLong(Global.miningInfo.height)
    md.update(seedBuffer.array())

    plot.hashScoop(md, scoopNum)
    val hash = md.digest()
    val hit = new BigInteger(1, Array[Byte](hash(7), 
      hash(6), hash(5), hash(4), hash(3), hash(2), hash(1), hash(0)))
    return hit.divide(BigInteger.valueOf(Global.miningInfo.baseTarget.toLong))
  }
  
  def calculateScoopNum {
    val scoopBuffer = ByteBuffer.allocate(32 + 8)
    var genSigBytes = new BigInteger(
        Global.miningInfo.generationSignature, 16).toByteArray
    scoopBuffer.put(genSigBytes)
    scoopBuffer.putLong(Global.miningInfo.height)
    md.reset()
    md.update(scoopBuffer.array())
    val scoopHash = new BigInteger(1, md.digest())
    scoopNum = scoopHash.mod(BigInteger.valueOf(SCOOPS_PER_PLOT)).intValue()
    scoopNumHeight = Global.miningInfo.height.toLong
  }

  class MiningPlot(addr: Long, nonce: Long) {        
    var data = new Array[Byte](PLOT_SIZE);
    val base_buffer = ByteBuffer.allocate(16)
    base_buffer.putLong(addr)
    base_buffer.putLong(nonce)
    val base: Array[Byte] = base_buffer.array()
    var sha = new Shabal256()
    var gendata = new Array[Byte](PLOT_SIZE + base.length)
    System.arraycopy(base, 0, gendata, PLOT_SIZE, base.length)
    for(i <- Range(PLOT_SIZE, PLOT_SIZE/HASH_SIZE, -HASH_SIZE)) {
      sha.reset()
      var len = PLOT_SIZE + base.length - i
      if(len > HASH_CAP) {
        len = HASH_CAP
      }
      sha.update(gendata, i, len)
      sha.digest(gendata, i - HASH_SIZE, HASH_SIZE)
    }
    sha.reset()
    sha.update(gendata)
    val finalhash: Array[Byte] = sha.digest()
    for(i <- 0 until PLOT_SIZE)
      data(i) = (gendata(i) ^ finalhash(i % HASH_SIZE)).asInstanceOf[Byte]

    def getScoop(pos: Int): Array[Byte] = {
      return java.util.Arrays.copyOfRange(
        data, pos * SCOOP_SIZE, (pos + 1) * SCOOP_SIZE)
    }
    
    def hashScoop(sha: Shabal256, pos: Int) {
      sha.update(data, pos * SCOOP_SIZE, SCOOP_SIZE)
    }
  }
}