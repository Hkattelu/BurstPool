package com.github.Chronox.pool.actors
import com.github.Chronox.pool.Global
import com.github.Chronox.pool.Config
import com.github.Chronox.pool.db.{User, Reward, Share}

import akka.actor.{ Actor, ActorLogging }
import akka.http.scaladsl.model._
import akka.stream.{ ActorMaterializer, ActorMaterializerSettings }
import scala.util.{ Failure, Success }
import scala.concurrent.duration._
import scala.collection.concurrent.TrieMap
import scala.collection.JavaConversions._
import java.util.concurrent.ConcurrentLinkedQueue
import java.lang.Long
import java.math.BigInteger
import scala.math.BigDecimal.RoundingMode

case class addShare(user: User, blockId: BigInteger,
  nonce: Long, deadline: Long)
case class dumpCurrentShares()
case class queueCurrentShares(blockId: BigInteger)
case class getCurrentPercents()
case class getAverageHistoricalPercents()

class ShareManager extends Actor with ActorLogging {
  var currentShares = TrieMap[User, Share]()
  val one = BigDecimal.valueOf(1)

  def receive() = {
    case addShare(user: User, blockId: BigInteger, 
      nonce: Long, deadline: Long) => {
      val share: Share = new Share(user.id, blockId, nonce, deadline)
      currentShares contains user match {
        case true => currentShares(user) = share
        case false => currentShares += (user->share)
      } 
    }
    case dumpCurrentShares() => {
      historicShareQueue.enqueue(currentShares clone)
      currentShares.clear()
    }
    case queueCurrentShares(blockId: BigInteger) => {
      Global.rewardPayout ! addRewards(blockId, 
        sharesToRewardPercents(currentShares.toMap), 
        historicShareQueue.getPercents())
      historicShareQueue.enqueue(currentShares clone)
      currentShares.clear()
    }
    case getCurrentPercents() => {
      sender ! sharesToRewardPercents(currentShares.toMap)
    }
    case getAverageHistoricalPercents() => {
      sender ! historicShareQueue.getPercents()
    }
  }

  def sharesToRewardPercents(weights: Map[User, Share]): 
    Map[Long, BigDecimal] = {
    val inverseWeights = weights.map{case (k,v) => (k.id, 
      one/BigDecimal.valueOf(v.deadline))}.asInstanceOf[Map[Long, BigDecimal]]
    val inverseSum = inverseWeights.values.sum
    return inverseWeights.map{case(k,v) => (
      k, (v/inverseSum).setScale(8, RoundingMode.HALF_EVEN))}
  }

  object historicShareQueue {
    var queue: ConcurrentLinkedQueue[TrieMap[User, Share]] = 
      new ConcurrentLinkedQueue[TrieMap[User, Share]]()

    def enqueue(map: TrieMap[User, Share]) {
      queue.add(map)
      if(queue.size() > Config.MIN_HEIGHT_DIFF) queue.poll()
    }

    def getPercents(): Map[Long, BigDecimal] = {
      val iterator = queue.iterator()
      var netHistoricPercents = 
        scala.collection.mutable.Map[Long, List[BigDecimal]]()
      while (iterator.hasNext()) {
        for ((id, percent) <- 
          sharesToRewardPercents(iterator.next().toMap[User, Share])) {
          if (netHistoricPercents contains id) 
            percent :: netHistoricPercents(id)
          else 
            netHistoricPercents += (id -> List[BigDecimal](percent))
        }
      }
      return netHistoricPercents.map{case(k,v) => (k, v.sum/v.length)}.toMap
    }
  }
}