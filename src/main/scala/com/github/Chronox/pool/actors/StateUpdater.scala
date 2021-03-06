package com.github.Chronox.pool.actors

import com.github.Chronox.pool.Global
import com.github.Chronox.pool.Config

import akka.actor.{ Actor, ActorLogging }
import akka.http.scaladsl.Http
import akka.http.scaladsl.model._
import akka.stream.{ ActorMaterializer, ActorMaterializerSettings }
import akka.util.ByteString
import scala.concurrent.duration._
import net.liftweb.json._
import language.postfixOps

case class StateTick()

class StateUpdater(isTest: Boolean) extends Actor with ActorLogging {

  import akka.pattern.pipe
  import context.dispatcher

  final implicit val materializer: ActorMaterializer = 
    ActorMaterializer(ActorMaterializerSettings(context.system))

  override def preStart() {
    context.system.scheduler.schedule(0 seconds, 1 minute, self, StateTick())
    if(isTest)
      context.system.scheduler.scheduleOnce(
        0 seconds, Global.miningInfoUpdater, getNewMiningInfo())
    else
      context.system.scheduler.schedule(
        0 seconds, 10 seconds, Global.miningInfoUpdater, getNewMiningInfo())
    context.system.scheduler.schedule(
      Config.PAY_TIME hours, Config.PAY_TIME hours, 
      Global.rewardPayout, payoutRewards())
  }

  def receive() = {
    case _: StateTick => {
      Global.burstPriceChecker ! updateBurstPriceInfo()
      Global.userManager ! refreshUsers()
    }
  }
}