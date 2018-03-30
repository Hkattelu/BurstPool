package com.github.Chronox.pool.actors

import com.github.Chronox.pool.db.Share

import akka.actor.{ Actor, ActorLogging }
import akka.util.Timeout
import scala.collection.concurrent.TrieMap
import scala.concurrent.duration._
import scala.util.{Failure, Success, Try}
import org.json4s.{DefaultFormats, Formats}
import java.math.BigInteger

case class addShares(blockId: BigInteger, shares: List[Share])
case class PayoutShares()

class UserPayout extends Actor with ActorLogging {

  protected implicit lazy val jsonFormats: Formats = 
    DefaultFormats.withBigDecimal
  protected implicit val timeout: Timeout = 5 seconds

  val burstToNQT = 100000000
  var sharesToPay = TrieMap[BigInteger, List[Share]]()

  def receive() = {
    case PayoutShares() => {
      
    }
    case addShares(blockId: BigInteger, shares: List[Share]) => {
      sharesToPay += (blockId->shares)
    }
  }
}