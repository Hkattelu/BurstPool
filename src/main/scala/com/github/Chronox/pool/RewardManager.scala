package com.github.Chronox.pool

import com.github.Chronox.pool.db.User
import com.github.Chronox.pool.db.Reward
import com.github.Chronox.pool.db.Share

import java.math.BigInteger

object RewardManager {

  var pendingPayments = scala.collection.mutable.Map[User, List[Share]]()
  var currentBlockPayments = scala.collection.mutable.Map[User, Share]()

  // Utilize blockreward value to calculate splits

  def updateRewardShares(accId: Long, blockId: BigInteger,
    nonce: Long, deadline: BigInteger) {

  }

  def calculateRewardPercent(deadline: BigInteger): Double = {
    return 0.0
  }

}