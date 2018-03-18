package com.github.Chronox.pool
import scala.concurrent.duration._
import scala.io.Source
import scala.util.parsing.json._
import java.math.BigInteger

object Config {
  var NODE_ADDRESS: String = ""
  var PRICE_ADDRESS: String = ""
  var POOL_FEE: Double = 0.0
  var CURRENT_BLOCK_SHARE: Double = 0.0
  var HISTORIC_BLOCK_SHARE: Double = 0.0
  var FEE_ADDRESS: String = ""
  var BAN_TIME: Int = 0
  var POOL_STRATEGY: String = ""
  var TARGET_DEADLINE: BigInteger = BigInteger.valueOf(0L)

  def init(): Boolean = {
    val fname = "/config.json"
    var json: String = ""
    for (line <- Source.fromURL(getClass.getResource(fname)).getLines)
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
        NODE_ADDRESS = elements("NODE_ADDRESS").asInstanceOf[String]
        PRICE_ADDRESS = elements("PRICE_ADDRESS").asInstanceOf[String]
        POOL_FEE = elements("POOL_FEE").asInstanceOf[String].toDouble
        CURRENT_BLOCK_SHARE = 
          elements("CURRENT_BLOCK_SHARE").asInstanceOf[String].toDouble
        HISTORIC_BLOCK_SHARE = 
          elements("HISTORIC_BLOCK_SHARE").asInstanceOf[String].toDouble
        FEE_ADDRESS = elements("FEE_ADDRESS").asInstanceOf[String]
        BAN_TIME = elements("BAN_TIME").asInstanceOf[String].toInt
        POOL_STRATEGY = elements("POOL_STRATEGY").asInstanceOf[String]
        TARGET_DEADLINE = new BigInteger(
          elements("TARGET_DEADLINE").asInstanceOf[String], 10)
        return true
      }
    }
  }
}