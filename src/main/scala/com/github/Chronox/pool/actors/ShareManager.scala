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
      Global.rewardPayout ! addShares(blockId, currentShares.values.toList)
    }
  }

  def weightsToPercents(weights: Map[Long, Long]): Map[Long, Double] = {
    val inverseWeights = 
      weights.map{case (k,v) => (k, 1/v)}.asInstanceOf[Map[Long, Double]]
    var inverseSum = 0.0
    for((k,v) <- inverseWeights) inverseSum += v
    return inverseWeights.map{ case (k,v) => (k, v/inverseSum)}
  }

  object historicShareQueue {
    var queue: ConcurrentLinkedQueue[TrieMap[User, Share]] = 
      new ConcurrentLinkedQueue[TrieMap[User, Share]]()
    val maxSize = Config.MIN_HEIGHT_DIFF

    def enqueue(map: TrieMap[User, Share]) {
      queue.add(map)
      if(queue.size() > maxSize) queue.poll()
    }

    def getIterator(): Iterator[TrieMap[User, Share]] = {
      return queue.iterator()
    }
  }
}