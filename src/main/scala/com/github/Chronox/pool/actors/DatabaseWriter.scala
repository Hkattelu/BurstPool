package com.github.Chronox.pool.actors
import akka.actor.Actor
import akka.stream.{ ActorMaterializer, ActorMaterializerSettings }

case class writeFunction(func: () => Any)
class DatabaseWriter extends Actor {
  def receive() = {
    case writeFunction(func) => func()
  }
}