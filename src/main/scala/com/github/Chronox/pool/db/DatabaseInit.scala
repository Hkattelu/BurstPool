package com.github.Chronox.pool.db
import com.github.Chronox.pool.Config
import com.mchange.v2.c3p0.ComboPooledDataSource
import org.squeryl.adapters.MySQLAdapter
import org.squeryl.Session
import org.squeryl.SessionFactory
import org.slf4j.LoggerFactory
import org.squeryl.PrimitiveTypeMode._

trait DatabaseInit {
  val logger = LoggerFactory.getLogger(getClass)
  var cpds = new ComboPooledDataSource

  def configureDb() {
    val databaseUsername = Config.DB_USER
    val databasePassword = Config.DB_PASS
    val databaseConnection = "jdbc:mysql://" + Config.DB_HOST + ":" + 
      Config.DB_PORT + "/" + Config.DB_NAME
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

    def connection = {Session.create(cpds.getConnection(), new MySQLAdapter)}

    SessionFactory.concreteFactory = Some(() => connection)
    try {transaction{PoolSchema.create}} 
    catch {case e: Throwable => println("Database already created")}
  }

  def closeDbConnection() {
    cpds.close()
  }
}