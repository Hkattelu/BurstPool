package com.github.Chronox.pool.actors

import com.github.Chronox.pool.Global
import com.github.Chronox.pool.Config
import com.github.Chronox.pool.db.User
import com.github.Chronox.pool.db.Reward
import com.github.Chronox.pool.db.Share

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

case class addShare(user: User, blockId: BigInteger,
  nonce: Long, deadline: Long)
case class dumpCurrentShares()
case class queueCurrentShares(blockId: BigInteger)

class ShareManager extends Actor with ActorLogging {
  var currentShares = TrieMap[User, Share]()

  def receive() = {
    case addShare(user: User, blockId: BigInteger, 
      nonce: Long, deadline: Long) => {
      val share: Share = new Share(user.id, blockId, nonce, deadline)
      if (currentShares contains user) currentShares(user) = share
      else currentShares += (user->share)
    }
    case dumpCurrentShares() => {
      historicShareQueue.enqueue(currentShares clone)
      currentShares.clear()
    }
    case queueCurrentShares(blockId: BigInteger) => {
      Global.rewardPayout ! addRewards(blockId, 
        sharesToRewardPercents(currentShares.toMap[User, Share]), 
        historicShareQueue.getPercents())
      currentShares.clear()
    }
  }

  def sharesToRewardPercents(weights: Map[User, Share]): Map[Long, Double] = {
    val inverseWeights = weights.map{case (k,v) => 
        (k.id, 1/v.deadline)}.asInstanceOf[Map[Long, Double]]
    var inverseSum = 0.0
    for((k,v) <- inverseWeights) inverseSum += v
    return inverseWeights.map{case(k,v) => (k, v/inverseSum)}
  }

  object historicShareQueue {
    var queue: ConcurrentLinkedQueue[TrieMap[User, Share]] = 
      new ConcurrentLinkedQueue[TrieMap[User, Share]]()
    val maxSize = Config.MIN_HEIGHT_DIFF

    def enqueue(map: TrieMap[User, Share]) {
      queue.add(map)
      if(queue.size() > maxSize) queue.poll()
    }

    def getPercents(): Map[Long, Double] = {
      val iterator = queue.iterator()
      var netHistoricPercents = 
        scala.collection.mutable.Map[Long, List[Double]]()
      while (iterator.hasNext()) {
        for ((id, percent) <- 
          sharesToRewardPercents(iterator.next().toMap[User, Share])) {
          if (netHistoricPercents contains id) 
            percent ::netHistoricPercents(id)
          else 
            netHistoricPercents += (id -> List[Double](percent))
        }
      }
      return netHistoricPercents.map{case(k,v) => (k, v.sum/v.length)}.toMap
    }
  }
}