package com.github.Chronox.pool.actors
import com.github.Chronox.pool.{Global, Config}
import com.github.Chronox.pool.db.User

import akka.actor.{ Actor, ActorLogging }
import akka.http.scaladsl.model._
import akka.stream.{ ActorMaterializer, ActorMaterializerSettings }
import akka.pattern.ask
import akka.util.Timeout
import scala.concurrent.duration._
import scala.util.{ Failure, Success }
import javax.servlet.http.HttpServletResponse
import java.math.BigInteger
import java.time.LocalDateTime

case class requestSubmission(ip: String, accId: Long, nonce: Long, 
  response: HttpServletResponse)
case class validateAndSendSubmission(ip: String, accId: Long,
  nonce: Long, user: User, response: HttpServletResponse)
class SubmissionHandler extends Actor with ActorLogging {

  import context.dispatcher
  final implicit val materializer: ActorMaterializer = 
    ActorMaterializer(ActorMaterializerSettings(context.system))
  protected implicit val timeout: Timeout = 5 seconds

  def receive() = {
    case requestSubmission(ip: String, accId: Long, nonce: Long, 
      response: HttpServletResponse) => {
      val s = sender
      // Check to see if this IP is banned
      val bannedFuture = (Global.userManager ? ipIsBanned(ip)).mapTo[Boolean]
      bannedFuture onComplete {
        case Success(false) => {
          // Check to see if we have a user with this ID
          val userFuture = (Global.userManager ? getUser(accId))
            .mapTo[Option[User]]
          userFuture onComplete {
            case Success(Some(user: User)) =>
              s ! (self ? validateAndSendSubmission(
                ip, accId, nonce, user, response))
            case Success(None) => {
              // Create a new user if one didn't exist with the given ID
              val addUserFuture = (Global.userManager ? addUser(ip, accId))
                .mapTo[Option[User]]
              addUserFuture onComplete {
                case Success(Some(user: User)) =>
                  s ! (self ? validateAndSendSubmission(
                    ip, accId, nonce, user, response))
                case Success(None) => {
                  response.setStatus(500)
                  s ! "Something went wrong, could not create new user"
                }
                case Failure(e: Throwable) => {
                  response.setStatus(500)
                  s ! ("Submit nonce error: " + e.toString)
                }
              }
            }
            case Failure(e: Throwable) => {
              response.setStatus(500)
              s ! ("Submit nonce error: " + e.toString)
            }
          }
        }
        case Success(true) =>  {
          response.setStatus(500)
          s ! "You IP is banned, you can't submit nonces"
        }
        case Failure(e: Throwable) =>  {
          response.setStatus(500)
          s ! "Submit nonce error: " + e.toString
        }
      }
    }
    case validateAndSendSubmission(ip: String, accId: Long,
    nonce: Long, user: User, response: HttpServletResponse) => {
      val s = sender
      val recipientFuture = (Global.userManager ? 
        checkRewardRecipient(accId)).mapTo[Boolean]
      recipientFuture onComplete {
        case Success(true) => {
          var deadline: BigInteger = 
            Config.TARGET_DEADLINE.add(BigInteger.valueOf(1L))
          val deadlineFuture = (Global.deadlineChecker ? nonceToDeadline(
            accId, nonce))(5 seconds).mapTo[BigInteger]
          deadlineFuture onComplete {
            case Success(deadline: BigInteger) => {
              if(deadline.compareTo(Config.TARGET_DEADLINE) <= 0) {
                // Submit nonce to network to verify
                val submitFuture = (Global.deadlineSubmitter ? submitNonce(
                  user, nonce, deadline))(5 seconds).mapTo[Result]
                submitFuture onComplete {
                  case Success(Result(Global.SUCCESS_MESSAGE)) => {
                    Global.userManager ! updateSubmitTime(accId)
                    s ! ("Deadline submission success: " + deadline.toString)
                  }
                  case Success(Result(message)) => {
                    response.setStatus(500)
                    s ! ("Deadline submission failure: " + message)
                  }
                  case Failure(e: Throwable) => {
                    response.setStatus(500)
                    s ! ("Failure while submitting deadline: " + e.toString)
                  }
                }
              } else {
                Global.userManager ! banUser(ip,
                  LocalDateTime.now().plusMinutes(Config.BAN_TIME))
                Global.poolStatistics.incrementBadNonces()
                response.setStatus(500)
                s ! ("You submitted a bad deadline, and are temporarily " + 
                "banned for " + Config.BAN_TIME + " minutes.")
              }
            }
            case Failure(e: Throwable) => {
              response.setStatus(500)
              s ! ("Failure on deadline calculation: " + e.toString)
            }
          }
        }
        case Success(false) => {
          response.setStatus(500)
          s ! ("You cannot submit nonces unless you reward recipient is" +
          " set to " + Config.ACCOUNT_ID + ". This can take up to 5" +
          " blocks to update")
        }
        case Failure(e: Throwable) => {
          response.setStatus(500)
          s ! ("Failure while getting reward recipient: " + e.toString)
        }
      }
    }
  }
}