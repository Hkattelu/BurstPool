package com.github.Chronox.pool.servlets

import akka.actor.ActorSystem
import org.scalatra._

class PoolServlet extends ScalatraServlet {

  get("/") {
    views.html.dashboard.render()
  }
}
