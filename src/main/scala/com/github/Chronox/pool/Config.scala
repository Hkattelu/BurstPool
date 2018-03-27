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
  var DAYS_UNTIL_INACTIVE: Int = 0
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
        NODE_ADDRESS = 
          elements.getOrElse("NODE_ADDRESS", "").asInstanceOf[String]
        PRICE_ADDRESS = 
          elements.getOrElse("PRICE_ADDRESS", "").asInstanceOf[String]
        POOL_FEE = 
          elements.getOrElse("POOL_FEE", 0.0).asInstanceOf[String].toDouble
        CURRENT_BLOCK_SHARE = 
          elements.getOrElse("CURRENT_BLOCK_SHARE", 0.0).asInstanceOf[String]
          .toDouble
        HISTORIC_BLOCK_SHARE = 
          elements.getOrElse("HISTORIC_BLOCK_SHARE", 0.0).asInstanceOf[String]
          .toDouble
        FEE_ADDRESS = elements.getOrElse("FEE_ADDRESS", "").asInstanceOf[String]
        BAN_TIME = elements.getOrElse("BAN_TIME", 0).asInstanceOf[String].toInt
        DAYS_UNTIL_INACTIVE = 
          elements.getOrElse("DAYS_UNTIL_INACTIVE", 5).asInstanceOf[String]
          .toInt
        POOL_STRATEGY = 
          elements.getOrElse("POOL_STRATEGY", "").asInstanceOf[String]
        TARGET_DEADLINE = new BigInteger(
          elements.getOrElse("TARGET_DEADLINE", BigInteger.valueOf(0L))
          .asInstanceOf[String], 10)
        return true
      }
    }
  }
}