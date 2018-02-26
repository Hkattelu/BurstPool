package com.github.Chronox.pool
import scala.concurrent.duration._
import scala.io.Source
import scala.util.parsing.json._
import java.math.BigInteger

object Config {
  var NODE_ADDRESS: Any = ""
  var PRICE_ADDRESS: Any = ""
  var POOL_FEE: Any = ""
  var CURRENT_BLOCK_SHARE: Any = ""
  var HISTORIC_BLOCK_SHARE: Any = ""
  var FEE_ADDRESS: Any = ""
  var BAN_TIME: Any = ""
  var POOL_STRATEGY: Any = ""
  var TARGET_DEADLINE: BigInteger = BigInteger.valueOf(0L)

  def init(): Boolean = {
    val fname = "config.json"
    var json: String = ""
    for (line <- Source.fromURL(getClass.getResource("/config.json")).getLines)
     json += line

    val result = try {
      JSON.parseFull(json)
    } catch {
      case ex: Exception => ex.printStackTrace()
    }

    result match {
      case None => {
        println("Invalid Config")
        return false
      }
      case Some(elements: Map[String, Any]) => {
        NODE_ADDRESS = elements("NODE_ADDRESS")
        PRICE_ADDRESS = elements("PRICE_ADDRESS")
        POOL_FEE = elements("POOL_FEE")
        CURRENT_BLOCK_SHARE = elements("CURRENT_BLOCK_SHARE")
        HISTORIC_BLOCK_SHARE = elements ("HISTORIC_BLOCK_SHARE")
        FEE_ADDRESS = elements("FEE_ADDRESS")
        BAN_TIME = elements("BAN_TIME")
        POOL_STRATEGY = elements("POOL_STRATEGY")
        TARGET_DEADLINE = new BigInteger(
          elements("TARGET_DEADLINE").asInstanceOf[String], 10)
        return true
      }
    }
  }
}