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

  val users = table[User]
  val blocks = table[Block] 
  val rewards = table[Reward]
  val shares = table[Share]
  val payments = table[PoolPayment]

  on(users)(u => declare(
    u.id is (indexed),
    u.lastSubmitHeight is (indexed),
    columns(u.id, u.nickName) are (unique)))
  on(blocks)(b => declare(
    b.generatorId is (indexed),
    b.height is (unique, indexed)))
  on(rewards)(r => declare(columns(r.userId, r.blockId) are (indexed)))
  on(shares)(s => declare(columns(s.blockId, s.userId) are (indexed)))

  val usersToShares = oneToManyRelation(users, shares).via(
    (u, s) => u.id === s.userId)
  val usersToRewards = oneToManyRelation(users, rewards).via(
    (u, r) => u.id === r.userId)
  val blocksToShares = oneToManyRelation(blocks, shares).via(
    (b, s) => b.id === s.blockId)
  val blocksToRewards = oneToManyRelation(blocks, rewards).via(
    (b, r) => b.id === r.blockId)

  usersToShares.foreignKeyDeclaration.constrainReference(onDelete cascade)
  usersToRewards.foreignKeyDeclaration.constrainReference(onDelete cascade)
  blocksToShares.foreignKeyDeclaration.constrainReference(onDelete cascade)
  blocksToRewards.foreignKeyDeclaration.constrainReference(onDelete cascade)

  override def applyDefaultForeignKeyPolicy(
    foreignKeyDeclaration: ForeignKeyDeclaration) =
  foreignKeyDeclaration.constrainReference

  def generateDB() = {transaction{this.create}}

  def clearAll() = {
    transaction{ 
      users.deleteWhere(u => 1 === 1)
      blocks.deleteWhere(b => 1 === 1)
      rewards.deleteWhere(r => 1 === 1)
      shares.deleteWhere(s => 1 === 1)
    }
  }

  def addUser(user: User): Boolean = {
    try {
      transaction {
        users.insert(user)
      }
    } catch { case e: Throwable => {
        println("Add user error: " + e.toString)
        return false
      }
    }
    return true
  }

  def getInactiveUser(accId: Long): Option[User] = {
    var user: Option[User] = None
    try {
      transaction {
        user = Some(users.where(
          u => u.id === accId and u.isActive === false).single)
      }
    } catch {case e: Throwable => {
        println("Get Inactive User error: " + e.toString)
        return None
      }
    }
    return user
  }

  def updateUsers(toUpdateUsers: List[User]) = {
    try { transaction { users.update(toUpdateUsers) } } 
    catch {case e: Throwable => { println("Update User error: " + e.toString) }}
  }

  def loadActiveUsers(): TrieMap[Long, User] = {
    var activeUsers = TrieMap[Long, User]()
    transaction {
      for(activeUser <- users.where(u => u.isActive === true))
        activeUsers += (activeUser.id->activeUser)
    }
    return activeUsers
  }

  def loadHistoricShares(): ConcurrentLinkedQueue[TrieMap[Long, Share]] = {
    var historicQueue = new ConcurrentLinkedQueue[TrieMap[Long, Share]]()
    transaction { 
      val recentBlocks = from(blocks)(b => 
        select(b) orderBy(b.height desc)).page(0, Config.MIN_HEIGHT_DIFF).toList
      var userShares = TrieMap[Long, Share]()
      for(block <- recentBlocks) 
        for(share <- shares.where(s => s.blockId === block.id).toList)
          userShares += (share.userId->share)   
        historicQueue.add(userShares)   
    }
    return historicQueue
  }

  def loadCurrentShares(): TrieMap[Long, Share] = {
    var userShares = TrieMap[Long, Share]()
    transaction {
      val currentBlock = from(blocks)(b => 
        select(b) orderBy(b.height desc)).page(0, 1).toList
      for (block <- currentBlock)
        for(share <- shares.where(s => s.blockId === block.id).toList)
          userShares += (share.userId->share)   
    }
    return userShares
  }

  def loadRewardShares(): TrieMap[Long, List[Reward]] = {
    var rewardsToPay = TrieMap[Long, List[Reward]]()
    transaction {
      var rewardList = rewards.where(r => r.isPaid === false).toList
      for(reward <- rewardList) {
        (rewardsToPay contains reward.userId) match {
          case true => reward :: rewardsToPay(reward.userId)
          case false => rewardsToPay += (reward.userId->List[Reward](reward))
        }
      }
    }
    return rewardsToPay
  }

  def loadPoolPayments(): TrieMap[Long, PoolPayment] = {
    var poolPayments = TrieMap[Long, PoolPayment]()
    transaction {
      var paymentList = from(payments)(p => select(p))
      for (payment <- paymentList)
        poolPayments += payment.id->payment
    }
    return poolPayments
  } 

  def addShareList(shareList: List[Share]) = {
    transaction{shares.insert(shareList)}
  }

  def addRewardList(rewardList: List[Reward]) = {
    transaction{rewards.insert(rewardList)}
  }

  def markRewardsAsPaid(rewardList: List[Reward]) =  {
    transaction{rewards.update(rewardList)}
  }

  def addBlock(block: Block) = {
    transaction{blocks.insert(block)}
  }

  def getBlock(id: Long): Option[Block] = {
    var block: Option[Block]  = None
    transaction{block = blocks.lookup(id)}
    return block
  }

  def blocksToRewardNQT(blockIds: List[Long]): Map[Long, Long] = {
    var blockToNQTMap = Map[Long, Long]()
    transaction{
      blockList = from(blocks)(b => 
        where(blockIds contains b.id) select(b))
      for(block <- blockList)
        blockToNQTMap += (block.id, (1-Config.POOL_FEE)*(
          block.blockReward * Global.burstToNQT + block.totalFeeNQT))
    }
    return blockToNQTMap
  }

  def addPayment(payment: PoolPayment) = {
    transaction{payments.insert(payment)}
  }

  def updatePayment(payment: PoolPayment) = {
    transaction{payments.update(payment)}
  }
}