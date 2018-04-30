package com.github.Chronox.pool.actors
import com.github.Chronox.pool.{Global, Config}
import com.github.Chronox.pool.db.{Share, Reward}

import akka.actor.{Actor, ActorLogging}
import akka.pattern.ask
import akka.stream.{ActorMaterializer, ActorMaterializerSettings}
import akka.util.{Timeout, ByteString}
import scala.util.{Failure, Success}
import scala.collection.concurrent.TrieMap
import scala.concurrent.{Future, Await}
import scala.concurrent.duration._
import language.postfixOps
import java.math.BigInteger

case class addRewards(blockId: Long, 
  currentSharePercents: Map[Long, BigDecimal],
  historicSharePercents: Map[Long, BigDecimal])
case class getUnpaidRewards()
case class clearUnpaidRewards()
case class dumpPaidRewards(paidRewards: List[Reward])

class RewardAccumulator extends Actor with ActorLogging {

  var unpaidRewards: TrieMap[Long, List[Reward]] = TrieMap[Long, List[Reward]]()
  var rewardsToRetry: Map[Long, List[Reward]] = Map[Long, List[Reward]]()
  val burstToNQT = 100000000L

  override def preStart() {
    unpaidRewards = Global.poolDB.loadRewardShares()
  }

  def receive() = {
    case addRewards(blockId: Long, 
      currentSharePercents: Map[Long, BigDecimal],
      historicSharePercents: Map[Long, BigDecimal]) => {
      // Add historical rewards for the block
      var rewards = historicSharePercents.map{case(k,v) => 
        (k, new Reward(k, blockId, 0.0, v, false))}

      // Add current share rewards for the block, if they exist
      for((id, percent) <- currentSharePercents) rewards contains id match {
        case true => rewards(id).currentPercent = percent
        case false => rewards += (
          id->(new Reward(id, blockId, percent, 0.0, false)))
      } 
      val rewardList = rewards.values.toList
      unpaidRewards += (blockId->rewardList)
      Global.dbWriter ! writeFunction(
        () => Global.poolDB.addRewardList(rewardList))
    }
    case getUnpaidRewards() => sender ! unpaidRewards.toMap
    case clearUnpaidRewards() => unpaidRewards.clear()
    case dumpPaidRewards(paidRewards: List[Reward]) => {
      unpaidRewards = unpaidRewards.map{
        case (k,v) => {
          (k, v diff paidRewards)
        }
      }
      unpaidRewards.retain((k,v) => !v.isEmpty)
    }
  }
}