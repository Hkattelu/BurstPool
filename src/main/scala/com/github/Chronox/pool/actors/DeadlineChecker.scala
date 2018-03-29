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
  
  def receive() = {
    case nonceToDeadline(accountId: Long, nonce: Long) => {
      val shabal = new Shabal256()
      sender ! getDeadline(generatePlot(accountId, nonce, shabal), 
        getScoopNum(shabal), shabal)
    }
  }

  def generatePlot(accountId: Long, nonce: Long, shabal: Shabal256): Array[Byte] = {
    shabal.reset()
    val seedBuffer = ByteBuffer.allocate(16) // 8 byte accId + 8 byte nonce
    seedBuffer.putLong(accountId)
    seedBuffer.putLong(nonce)

    val seed = seedBuffer.array()
    val gendata = new Array[Byte](PLOT_SIZE + seed.length)
    System.arraycopy(seed, 0, gendata, PLOT_SIZE, seed.length)
    for{i <- PLOT_SIZE/HASH_SIZE until 0} {
      shabal.reset()
      var len = PLOT_SIZE + seed.length - i*HASH_SIZE
      if(len > HASH_CAP) {
        len = HASH_CAP
      }
      shabal.update(gendata, HASH_SIZE*i, len)
      shabal.digest(gendata, HASH_SIZE*(i - 1), HASH_SIZE)
    }
    shabal.reset()
    shabal.update(gendata)
    val finalhash = shabal.digest()
    var plot = new Array[Byte](PLOT_SIZE)
    for{i <- 0 until PLOT_SIZE} {
      plot(i) = (gendata(i) ^ finalhash(i % HASH_SIZE)).asInstanceOf[Byte]
    }
    return plot
  }

  def getDeadline(plot: Array[Byte], scoopNum: Int, shabal: Shabal256): BigInteger = {
    shabal.reset()
    val genSigBuffer = ByteBuffer.allocate(32)
    val genSigBytes = new BigInteger(
      Global.miningInfo.generationSignature, 16).toByteArray
    genSigBuffer.put(genSigBytes)

    shabal.update(genSigBuffer.array())
    shabal.update(plot, scoopNum * SCOOP_SIZE, SCOOP_SIZE);
    val hash = shabal.digest()

    val hit = new BigInteger(1, Array[Byte](hash(7), hash(6), hash(5), hash(4),
      hash(3), hash(2), hash(1), hash(0)))
    return hit.divide(BigInteger.valueOf(Global.miningInfo.baseTarget.toLong))
  }

  def getScoopNum(shabal: Shabal256): Int = {
    val seedBuffer = ByteBuffer.allocate(40) // 32 byte gensig + 8 byte height

    //Race condition: Fails if the mining info response hasn't been received yet
    val genSigBytes = new BigInteger(
      Global.miningInfo.generationSignature, 16).toByteArray
    seedBuffer.put(genSigBytes)
    seedBuffer.putLong(Global.miningInfo.height)
    shabal.reset()
    shabal.update(seedBuffer.array())
    val generationHash = new BigInteger(1,shabal.digest())
    return generationHash.mod(
      BigInteger.valueOf(SCOOPS_PER_PLOT)).intValue()
  }
}