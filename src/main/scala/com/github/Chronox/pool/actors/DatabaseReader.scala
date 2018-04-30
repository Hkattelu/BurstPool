package com.github.Chronox.pool.actors
import akka.actor.Actor
import akka.stream.{ ActorMaterializer, ActorMaterializerSettings }

case class readFunction(func: () => Any)
class DatabaseReader extends Actor {
  def receive() = {
    case readFunction(func) => sender ! func()
  }
}