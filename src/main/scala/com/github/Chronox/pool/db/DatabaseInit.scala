package com.github.Chronox.pool.db

import com.mchange.v2.c3p0.ComboPooledDataSource
import org.squeryl.adapters.MySQLAdapter
import org.squeryl.Session
import org.squeryl.SessionFactory
import org.slf4j.LoggerFactory
import org.squeryl.PrimitiveTypeMode._

trait DatabaseInit {
  val logger = LoggerFactory.getLogger(getClass)

  val databaseUsername = "root"
  val databasePassword = ""
  val databaseConnection = "jdbc:mysql://localhost:3306/pool"

  var cpds = new ComboPooledDataSource

  def configureDb() {
    cpds.setDriverClass("com.mysql.jdbc.Driver")
    cpds.setJdbcUrl(databaseConnection)
    cpds.setUser(databaseUsername)
    cpds.setPassword(databasePassword)

    cpds.setMinPoolSize(1)
    cpds.setInitialPoolSize(4)
    cpds.setNumHelperThreads(4)
    cpds.setAcquireIncrement(1)
    cpds.setMaxPoolSize(50)
    cpds.setTestConnectionOnCheckin(true)

    def connection = {
      Session.create(
      //  java.sql.DriverManager.getConnection(
      //  databaseConnection, databaseUsername, databasePassword),
        cpds.getConnection(),
        new MySQLAdapter)
    }

    SessionFactory.concreteFactory = Some(() => connection)

    transaction {
      PoolSchema.create
    }
  }

  def closeDbConnection() {
    cpds.close()
  }
}