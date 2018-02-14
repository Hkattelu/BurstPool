package com.github.Chronox.pool

import org.squeryl.PrimitiveTypeMode._
import org.squeryl.Schema
import org.squeryl.annotations.Column
import java.util.Date
import java.sql.Timestamp

object PoolSchema extends Schema {
  val users = table[User]
  val blocks = table[Block] 
  val rewards = table[Reward]
  val shares = table[Share]
  val pool = table[Pool]
}