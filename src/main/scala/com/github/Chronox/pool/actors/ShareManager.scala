package com.github.Chronox.pool.actors
import com.github.Chronox.pool.{ Global, Config }
import com.github.Chronox.pool.db.{ User, Reward, Share }

import akka.actor.{ Actor, ActorLogging }
import akka.http.scaladsl.model._
import akka.stream.{ ActorMaterializer, ActorMaterializerSettings }
import scala.util.{ Failure, Success }
import scala.concurrent.duration._
import scala.collection.concurrent.TrieMap
import scala.collection.JavaConversions._
import java.util.concurrent.ConcurrentLinkedQueue
import scala.math.BigDecimal.RoundingMode
import language.postfixOps

case class addShare(user: User, blockId: Long, nonce: Long, deadline: Long)
case class dumpCurrentShares()
case class queueCurrentShares(blockId: Long)
case class getCurrentPercents()
case class getAverageHistoricalPercents()

class ShareManager extends Actor with ActorLogging {
  var currentShares: TrieMap[Long, Share] = TrieMap[Long, Share]()
  val one = BigDecimal.valueOf(1)

  override def preStart() {
    historicShareQueue.init(Global.poolDB.loadHistoricShares())
    currentShares = Global.poolDB.loadCurrentShares()
  }

  def receive() = {
    case addShare(user: User, blockId: Long, 
      nonce: Long, deadline: Long) => {
      val share: Share = new Share(user.id, blockId, nonce, Some(deadline))
      currentShares contains user.id match {
        case true => currentShares(user.id) = share
        case false => currentShares += (user.id->share)
      }
    }
    case dumpCurrentShares() => {
      historicShareQueue.enqueue(currentShares clone)
      Global.dbWriter ! writeFunction(
        () => Global.poolDB.addShareList(currentShares.values.toList))
      currentShares.clear()
    }
    case queueCurrentShares(blockId: Long) => {
      Global.rewardPayout ! addRewards(blockId, 
        sharesToRewardPercents(currentShares.toMap), 
        historicShareQueue.getPercents())
      self ! dumpCurrentShares()
    }
    case getCurrentPercents() => {
      sender ! sharesToRewardPercents(currentShares.toMap)
    }
    case getAverageHistoricalPercents() => {
      sender ! historicShareQueue.getPercents()
    }
  }

  def sharesToRewardPercents(weights: Map[Long, Share]): 
    Map[Long, BigDecimal] = {
    val inverseWeights = weights.map{case (k,v) => (k, 
      one/BigDecimal.valueOf(v.deadline.get))}
      .asInstanceOf[Map[Long, BigDecimal]]
    val inverseSum = inverseWeights.values.sum
    return inverseWeights.map{case(k,v) => (
      k, (v/inverseSum).setScale(8, RoundingMode.HALF_EVEN))}
  }

  object historicShareQueue {
    var queue: ConcurrentLinkedQueue[TrieMap[Long, Share]] = 
      new ConcurrentLinkedQueue[TrieMap[Long, Share]]()
    def init(q: ConcurrentLinkedQueue[TrieMap[Long, Share]]) { queue = q }

    def enqueue(map: TrieMap[Long, Share]) {
      queue.add(map)
      if(queue.size() > Config.MIN_HEIGHT_DIFF) queue.poll()
    }

    def getPercents(): Map[Long, BigDecimal] = {
      val iterator = queue.iterator()
      var netHistoricPercents = 
        scala.collection.mutable.Map[Long, List[BigDecimal]]()
      while (iterator.hasNext())
        for ((id, percent) <- sharesToRewardPercents(
          iterator.next().toMap[Long, Share])) {
          (netHistoricPercents contains id) match {
            case true => netHistoricPercents(id) = 
              percent :: netHistoricPercents(id)
            case false => netHistoricPercents += (id->List[BigDecimal](percent))
          }
        }
      return netHistoricPercents.map{case(k,v) => (k, v.sum/v.length)}.toMap
    }
  }
}