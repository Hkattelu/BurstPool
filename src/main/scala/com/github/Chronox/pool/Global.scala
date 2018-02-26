package com.github.Chronox.pool

import actors._

import scala.concurrent.ExecutionContext.Implicits.global
import akka.actor.ActorRef
import scala.collection.concurrent.TrieMap

object Global {

  var stateUpdater: ActorRef = null
  var burstPriceChecker: ActorRef = null
  var miningInfoUpdater: ActorRef = null
  var deadlineSubmitter: ActorRef = null
  val userManager = UserManager
  val deadlineChecker = DeadlineChecker

  var miningInfo: MiningInfo = MiningInfo(
    null, null, 0L, null, null, null, 0L, 0L)
  var difficulty: Difficulty = Difficulty(null)
  var burstPriceInfo : BurstPriceInfo = BurstPriceInfo("Not found", "Not found")
}