package com.github.Chronox.pool

import scala.concurrent.ExecutionContext.Implicits.global
import akka.actor.ActorRef
import scala.collection.concurrent.TrieMap

object Global {
  var stateUpdater: ActorRef = null
  var burstPriceChecker: ActorRef = null
  var lastBlockGetter: ActorRef = null
  var deadlineSubmitter: ActorRef = null
  var userManager: ActorRef = null

  var miningInfo: MiningInfo = MiningInfo(null, null, null, null)
  var burstInfo : BurstPriceInfo = BurstPriceInfo("Not found", "Not found")
}