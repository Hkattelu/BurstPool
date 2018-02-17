package com.github.Chronox.pool

import akka.actor.{ Actor, ActorLogging }
import akka.http.scaladsl.Http
import akka.http.scaladsl.model._
import akka.stream.{ ActorMaterializer, ActorMaterializerSettings }
import akka.util.ByteString
import scala.concurrent.duration._
import net.liftweb.json._

case class addUser(ip_address: String, accountId:String)
case class containsUser(ip_address: String)
case class banUser(ip_address: String)
case class refreshUsers()

class UserManager extends Actor with ActorLogging {

  import akka.pattern.pipe
  import context.dispatcher

  implicit val formats = DefaultFormats
  final implicit val materializer: ActorMaterializer = 
    ActorMaterializer(ActorMaterializerSettings(context.system))

  var activeUsers = scala.collection.mutable.Map[String, User]()

  def receive() = {
    case banUser(ip_address: String) => {}
    case constainsUser(ip_address: String) => {}
    case addUser(ip_address: String, accountId: String) => {}
    case refreshUsers() => {}
  }
}