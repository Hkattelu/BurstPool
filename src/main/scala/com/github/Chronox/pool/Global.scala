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
  var rewardAccumulator: ActorRef = null
  var rewardPayout: ActorRef = null
  var paymentLedger: ActorRef = null
  var dbWriter: ActorRef = null
  var dbReader: ActorRef = null

  val poolStatistics = PoolStatistics
  val poolDB = PoolSchema
  val burstToNQT = 100000000L

  case class MiningInfo(generationSignature:String, baseTarget: String, 
    height: String)
  case class LastBlockInfo(generationSignature:String, block: String,
    nonce: String, baseTarget: String, height: String, blockReward: String,
    generator: String, generatorRS: String, timestamp: String)
  case class ErrorMessage(errorCode: String, errorDescription: String)
  case class setSubmitURI(uri: String)

  val SUCCESS_MESSAGE = "success"
  var currentBestDeadline: BigInteger = Config.TARGET_DEADLINE
  var miningInfo: MiningInfo = MiningInfo(null, null, null)
  var lastBlockInfo: LastBlockInfo = LastBlockInfo(null, null, null, null, 
    null, null, null, null, null)
  var burstPriceInfo : BurstPriceInfo = BurstPriceInfo("Not found", "Not found")
}