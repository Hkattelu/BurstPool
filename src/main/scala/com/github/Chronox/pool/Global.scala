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
  val userManager = UserManager
  val rewardManager = RewardManager
  val poolStatistics = PoolStatistics
  val deadlineChecker = DeadlineChecker

  var currentBestDeadline: BigInteger = Config.TARGET_DEADLINE
  var miningInfo: MiningInfo = MiningInfo(
    null, null, 0L, null, null, null, null)
  var burstPriceInfo : BurstPriceInfo = BurstPriceInfo("Not found", "Not found")
}