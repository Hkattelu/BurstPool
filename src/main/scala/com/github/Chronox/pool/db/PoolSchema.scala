package com.github.Chronox.pool.db
import com.github.Chronox.pool.{Global, Config}

import org.squeryl.PrimitiveTypeMode._
import org.squeryl.dsl.NumericalExpression
import org.squeryl.dsl.ast.TypedExpressionNode
import org.squeryl.Schema
import org.squeryl.ForeignKeyDeclaration
import language.postfixOps
import scala.collection.concurrent.TrieMap
import java.util.concurrent.ConcurrentLinkedQueue
  
object PoolSchema extends Schema {

  // Table declarations
  val users = table[User]
  val blocks = table[Block] 
  val rewards = table[Reward]
  val shares = table[Share]
  val pool = table[Pool]

  // Index and candidate key delcarations
  on(users)(u => declare(
    u.id is (indexed),
    u.lastSubmitHeight is (indexed),
    columns(u.id, u.nickName) are (unique)))
  on(blocks)(b => declare(
    b.generator is (indexed),
    b.height is (unique, indexed)))
  on(rewards)(r => declare(columns(r.userId, r.blockId) are (indexed)))
  on(shares)(s => declare(columns(s.blockId, s.userId) are (indexed)))

  // One to many relations
  val usersToShares = oneToManyRelation(users, shares).via(
    (u, s) => u.id === s.userId)
  val usersToRewards = oneToManyRelation(users, rewards).via(
    (u, r) => u.id === r.userId)
  val blocksToShares = oneToManyRelation(blocks, shares).via(
    (b, s) => b.id === s.blockId)
  val blocksToRewards = oneToManyRelation(blocks, rewards).via(
    (b, r) => b.id === r.blockId)

  // Foreign Key constraints
  usersToShares.foreignKeyDeclaration.constrainReference(onDelete cascade)
  usersToRewards.foreignKeyDeclaration.constrainReference(onDelete cascade)
  blocksToShares.foreignKeyDeclaration.constrainReference(onDelete cascade)
  blocksToRewards.foreignKeyDeclaration.constrainReference(onDelete cascade)

  override def applyDefaultForeignKeyPolicy(
    foreignKeyDeclaration: ForeignKeyDeclaration) =
  foreignKeyDeclaration.constrainReference

  // Queries
  def loadActiveUsers(): TrieMap[String, User] = {
    var activeUsers = TrieMap[String, User]()
    for(activeUser <- users.where(u => u.isActive === true))
      activeUsers += (activeUser.ip->activeUser)
    return activeUsers
  }

  def loadHistoricShares(): ConcurrentLinkedQueue[TrieMap[Long, Share]] = {
    var historicQueue = new ConcurrentLinkedQueue[TrieMap[Long, Share]]()
      for(i <- 1 to Config.MIN_HEIGHT_DIFF) {
        transaction {   
          val block = blocks.where(
            b => b.height === (Global.miningInfo.height.toLong - i)).single
          var userShares = TrieMap[Long, Share]()
          if (block != None) {
            for(share <- shares.where(s => s.blockId === block.id).toList)
              userShares += (share.userId->share)   
          }
          historicQueue.add(userShares)
        }   
      }
    return historicQueue
  }

  def loadCurrentShares(): TrieMap[Long, Share] = {
    var userShares = TrieMap[Long, Share]()
    transaction {
      val block = blocks.where(
        b => b.height === (Global.miningInfo.height.toLong - 1)).single
      if (block != None) {
        for(share <- shares.where(s => s.blockId === block.id).toList)
          userShares += (share.userId->share)   
      }
    }
    return userShares
  }

  def loadRewardShares(): TrieMap[Long, List[Reward]] = {
    var rewardsToPay = TrieMap[Long, List[Reward]]()
    transaction {
      var rewardList = rewards.where(r => r.isPaid === false).toList
      for (reward <- rewardList) {
        (rewardsToPay contains reward.userId) match {
          case true => reward :: rewardsToPay(reward.userId)
          case false => rewardsToPay += (reward.userId->List[Reward](reward))
        }
      }
    }
    return rewardsToPay
  }
}