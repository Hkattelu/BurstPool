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
  nonce: Long, deadline: BigInteger)
case class dumpCurrentShares()
case class queueCurrentShares(blockId: BigInteger)

class RewardManager extends Actor with ActorLogging {

  var currentBlockShares = scala.collection.concurrent.TrieMap[User, Share]()

  def receive() = {
    case addShare(user: User, blockId: BigInteger, 
      nonce: Long, deadline: BigInteger) => {
      val share: Share = new Share(user.id, blockId, nonce, deadline, false)
      if (currentBlockShares contains user)
        currentBlockShares(user) = share
      else
        currentBlockShares += (user->share)
    }
    case dumpCurrentShares() => currentBlockShares.clear()
    case queueCurrentShares(blockId: BigInteger) => {
      var blockShares = List[Share]()
      for ((k,v) <- currentBlockShares) {v :: blockShares}
      Global.userPayout ! addShares(blockId, blockShares)
    }
  }
}