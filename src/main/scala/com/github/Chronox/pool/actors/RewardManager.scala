package com.github.Chronox.pool.actors

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

case class updateRewardShares(accId: Long, blockId: BigInteger,
  deadline: BigInteger)

class RewardManager extends Actor with ActorLogging {

  var currentBlockDeadlines = 
    scala.collection.concurrent.TrieMap[User, BigInteger]()

  var last500Shares = scala.collection.concurrent.TrieMap[User, List[Share]]()
  var currentBlockShares = scala.collection.concurrent.TrieMap[User, Share]()

  def receive() = {
    case updateRewardShares(accId: Long, blockId: BigInteger,
      deadline: BigInteger) => {
      
    }
  }

}