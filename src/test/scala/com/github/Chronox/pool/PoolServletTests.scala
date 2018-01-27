package com.github.Chronox.pool

import org.scalatra.test.scalatest._

class PoolServletTests extends ScalatraFunSuite {

  addServlet(classOf[PoolServlet], "/*")

  test("GET / on PoolServlet should return status 200"){
    get("/"){
      status should equal (200)
    }
  }

}
