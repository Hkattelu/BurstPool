package com.github.Chronox.pool

import akka.actor.ActorSystem
import org.scalatra._
import scala.concurrent.{ExecutionContext, Future, Promise}
import scala.util.{Failure, Success, Try}

class PoolServlet(system: ActorSystem) extends ScalatraServlet {

  get("/") {
    views.html.dashboard.render()
  }
}
