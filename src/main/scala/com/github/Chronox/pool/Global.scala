package com.github.Chronox.pool

import scala.concurrent.ExecutionContext.Implicits.global
import akka.actor.ActorRef
import scala.collection.concurrent.TrieMap

object Global {
  val SCOOPS_PER_PLOT = 4096

  var stateUpdater: ActorRef = null
  var burstPriceChecker: ActorRef = null
  var miningInfoUpdater: ActorRef = null
  var deadlineSubmitter: ActorRef = null
  val userManager = UserManager
  val deadlineChecker = DeadlineChecker

  var miningInfo: MiningInfo = MiningInfo(null, null, 0L, null, null)
  var difficulty: Difficulty = Difficulty(null)
  var burstPriceInfo : BurstPriceInfo = BurstPriceInfo("Not found", "Not found")
}