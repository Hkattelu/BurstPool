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
  var PAY_TIME: Int = 0
  var ACCOUNT_ID: String = ""
  var SECRET_PHRASE: String = ""
  var MIN_HEIGHT_DIFF: Int = 0
  var POOL_STRATEGY: String = ""
  var TARGET_DEADLINE: BigInteger = BigInteger.valueOf(0L)
  var DB_USER: String = ""
  var DB_PASS: String = ""
  var DB_HOST: String = "" 
  var DB_PORT: String = ""
  var DB_NAME: String = ""

  def init(): Boolean = {
    val fname = "/config.json"
    var json: String = ""
    for (line <- Source.fromURL(getClass.getResource(fname)).getLines)
      json += line

    val result = try {JSON.parseFull(json)} 
      catch {case ex: Exception => ex.printStackTrace()}

    result match {
      case None => {
        println("Invalid Config")
        return false
      }
      case Some(map) => {
        val elements = map.asInstanceOf[Map[String, String]]
        NODE_ADDRESS = elements.getOrElse("NODE_ADDRESS", "")
        PRICE_ADDRESS = elements.getOrElse("PRICE_ADDRESS", "")
        POOL_FEE = elements.getOrElse("POOL_FEE", "0.02").toDouble
        CURRENT_BLOCK_SHARE = 
          elements.getOrElse("CURRENT_BLOCK_SHARE", "0.18").toDouble
        HISTORIC_BLOCK_SHARE = 
          elements.getOrElse("HISTORIC_BLOCK_SHARE", "0.80").toDouble
        BAN_TIME = elements.getOrElse("BAN_TIME", "3").toInt
        PAY_TIME = elements.getOrElse("PAY_TIME", "5").toInt
        ACCOUNT_ID = elements.getOrElse("ACCOUNT_ID", "")
        SECRET_PHRASE = elements.getOrElse("SECRET_PHRASE", "")
        MIN_HEIGHT_DIFF = elements.getOrElse("MIN_HEIGHT_DIFF", "200").toInt
        POOL_STRATEGY = elements.getOrElse("POOL_STRATEGY", "STANDARD")
        TARGET_DEADLINE = BigInteger.valueOf(
          elements.getOrElse("TARGET_DEADLINE", "2592000").toLong)
        DB_USER = elements.getOrElse("DB_USER", "root")
        DB_PASS = elements.getOrElse("DB_PASS", "")
        DB_HOST = elements.getOrElse("DB_HOST", "localhost")
        DB_PORT = elements.getOrElse("DB_PORT", "3306")
        DB_NAME = elements.getOrElse("DB_NAME", "ChronoxPool")
        Global.poolInfo = Global.PoolInfo(ACCOUNT_ID, 100*POOL_FEE, 
          100*CURRENT_BLOCK_SHARE, 100*HISTORIC_BLOCK_SHARE, MIN_HEIGHT_DIFF,
          PAY_TIME, BAN_TIME)
        return true
      }
    }
  }
}