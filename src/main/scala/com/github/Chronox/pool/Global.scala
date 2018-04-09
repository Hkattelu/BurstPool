package com.github.Chronox.pool

import actors._

import scala.concurrent.ExecutionContext.Implicits.global
import akka.actor.ActorRef
import scala.collection.concurrent.TrieMap
import java.math.BigInteger

object Global {
  var stateUpdater: ActorRef = null
  var burstPriceChecker: ActorRef = null
  var miningInfoUpdater: ActorRef = null
  var deadlineSubmitter: ActorRef = null
  var deadlineChecker: ActorRef = null
  var userManager: ActorRef = null
  var shareManager: ActorRef = null
  var rewardPayout: ActorRef = null
  val poolStatistics = PoolStatistics

  var currentBestDeadline: BigInteger = Config.TARGET_DEADLINE
  var miningInfo: MiningInfo = MiningInfo(
    null, null, null, 0L, null, null, null, null)
  var burstPriceInfo : BurstPriceInfo = BurstPriceInfo("Not found", "Not found")

  case class MiningInfo(generationSignature:String, block: String,
    baseTarget: String, height: Long, blockReward: String,
    generator: String, generatorRS: String,
    numberOfTransactions: String)
}