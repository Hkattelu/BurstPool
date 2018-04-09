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
  on(shares)(s => declare(columns(s.userId, s.blockId) are (indexed)))

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
    for (activeUser <- users.where(u => u.isActive === true))
      activeUsers += (activeUser.ip->activeUser)
    return activeUsers
  }

  // TODO: fix this
  def loadHistoricShares(): ConcurrentLinkedQueue[TrieMap[Long, Share]] = {
    var historicQueue = new ConcurrentLinkedQueue[TrieMap[Long, Share]]()
    var historicBlocks = blocks.where(
      // b => b.height > Global.miningInfo.height - Config.MIN_HEIGHT_DIFF
      b => b.height === 0
      ).toList
    for(block <- historicBlocks){
      var userShares = TrieMap[Long, Share]()
      for(share <- shares.where(s => s.blockId === block.id).toList)
        userShares += (share.userId->share)      
      historicQueue.add(userShares)
    }
    return historicQueue
  }

  def loadCurrentShares(): TrieMap[Long, Share] = {
    return null
  }

  def loadRewardShares() {}

  def loadBlockToNQTMap() {}
}