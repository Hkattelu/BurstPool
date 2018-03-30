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
import java.math.BigInteger

case class addShare(user: User, blockId: BigInteger,
  nonce: Long, deadline: Long)
case class dumpCurrentShares()
case class queueCurrentShares(blockId: BigInteger)

class RewardManager extends Actor with ActorLogging {
  var currentShares = TrieMap[User, Share]()

  def receive() = {
    case addShare(user: User, blockId: BigInteger, 
      nonce: Long, deadline: Long) => {
      val share: Share = new Share(user.id, blockId, nonce, deadline, false)
      if (currentShares contains user) currentShares(user) = share
      else currentShares += (user->share)
    }
    case dumpCurrentShares() => currentShares.clear()
    case queueCurrentShares(blockId: BigInteger) => {
      Global.userPayout ! addShares(blockId, currentShares.values.toList)
    }
  }
}