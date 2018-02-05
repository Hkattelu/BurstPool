package com.github.Chronox.pool

import scala.concurrent.ExecutionContext.Implicits.global
import akka.actor.ActorRef
import scala.collection.concurrent.TrieMap

object Global {
  var stateUpdater: ActorRef = null
  var burstChecker: ActorRef = null
  var lastBlockGetter: ActorRef = null
  var deadlineChecker: ActorRef = null
  var deadlineSubmitter: ActorRef = null

  var currentBlock: Block = null
  var burstInfo : BurstPriceInfo = BurstPriceInfo("Not found", "Not found")
}