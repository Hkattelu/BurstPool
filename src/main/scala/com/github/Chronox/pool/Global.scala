package com.github.Chronox.pool

import actors._

import scala.concurrent.ExecutionContext.Implicits.global
import akka.actor.ActorRef
import scala.collection.concurrent.TrieMap

object Global {

  var stateUpdater: ActorRef = null
  var burstPriceChecker: ActorRef = null
  var miningInfoUpdater: ActorRef = null
  var deadlineSubmitter = DeadlineSubmitter
  val userManager = UserManager
  val rewardManager = RewardManager
  val poolStatistics = PoolStatistics
  val deadlineChecker = DeadlineChecker

  var miningInfo: MiningInfo = MiningInfo(
    null, null, 0L, null, null, null, null)
  var burstPriceInfo : BurstPriceInfo = BurstPriceInfo("Not found", "Not found")
}