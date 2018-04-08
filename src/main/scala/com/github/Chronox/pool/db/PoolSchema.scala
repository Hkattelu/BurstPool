package com.github.Chronox.pool.db
import org.squeryl.PrimitiveTypeMode._
import org.squeryl.dsl.NumericalExpression
import org.squeryl.dsl.ast.TypedExpressionNode
import org.squeryl.Schema
import org.squeryl.annotations.Column

object PoolSchema extends Schema {
  val users = table[User]
  val blocks = table[Block] 
  val rewards = table[Reward]
  val shares = table[Share]
  val pool = table[Pool]

  val usersToShares = oneToManyRelation(users, shares).via(
    (u, s) => u.id === s.userId)

  val usersToRewards = oneToManyRelation(users, rewards).via(
    (u, r) => u.id === r.userId)

  val blocksToShares = oneToManyRelation(blocks, shares).via(
    (b, s) => b.id === s.blockId)

  val blocksToRewards = oneToManyRelation(blocks, rewards).via(
    (b, r) => b.id === r.blockId)

  on(users)(u => declare(
    u.id is (indexed),
    columns(u.id, u.nickName) are (unique)
  ))

  on(blocks)(b => declare(
    b.generator is (indexed)
  ))

  on(rewards)(r => declare(
    columns(r.userId, r.blockId) are (indexed)
  ))

  on(shares)(s => declare(
    columns(s.userId, s.blockId) are (indexed)
  ))

}