package com.github.Chronox.pool
import scala.concurrent.duration._

object Config {
  val POOL_FEE = 0.02
  val CURRENT_BLOCK_SHARE = 0.18
  val HISTORIC_BLOCK_SHARE = 0.80

  val FEE_ADDRESS = "BURST-EJH3-T42U-AWMF-7NBDR"
  val BAN_TIME = 60 seconds
  val POOL_STRATEGY = "STANDARD"
  val targetDeadline = 24 days
}