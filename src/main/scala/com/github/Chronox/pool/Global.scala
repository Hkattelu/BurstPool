package com.github.Chronox.pool
import actors._
import db.PoolSchema

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
  val poolDB = PoolSchema

  var currentBestDeadline: BigInteger = Config.TARGET_DEADLINE
  var miningInfo: MiningInfo = MiningInfo(null, null, null)
  var lastBlockInfo: LastBlockInfo = LastBlockInfo(null, null, null, null, 
    null, null, null, null)
  var burstPriceInfo : BurstPriceInfo = BurstPriceInfo("Not found", "Not found")

  case class MiningInfo(generationSignature:String, baseTarget: String, 
    height: String)
  case class LastBlockInfo(generationSignature:String, block: String,
    baseTarget: String, height: String, blockReward: String,
    generator: String, generatorRS: String,
    numberOfTransactions: String)
  case class ErrorMessage(errorCode: String, errorDescription: String)

  val SUCCESS_MESSAGE = "success"
}