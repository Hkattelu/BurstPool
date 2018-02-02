package com.github.Chronox.pool

import scala.concurrent.ExecutionContext.Implicits.global
import akka.actor.ActorRef
import scala.collection.concurrent.TrieMap

object Global {
  var stateUpdater: ActorRef = null
  var burstChecker: ActorRef = null
  var lastBlockGetter: ActorRef = null
}