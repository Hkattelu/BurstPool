package com.github.Chronox.pool.actors

import com.github.Chronox.pool.db.Share

import akka.actor.{ Actor, ActorLogging }
import akka.util.Timeout
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

  def receive() = {
    case PayoutShares() => {
      //Payout the users by the rewardmanager, create tx's and send them
    }
    case addShares(blockId: BigInteger, shares: List[Share]) => {}
  }
}