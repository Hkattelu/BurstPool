package com.github.Chronox.pool

object PaySplitStrategy extends Enumeration {
  type PaySplitStrategy = String
  val STANDARD = "STANDARD"
  val EVEN = "EVEN"
  val TB = "TB"
}