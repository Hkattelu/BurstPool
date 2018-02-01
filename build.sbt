val ScalatraVersion = "2.6.2"

organization := "com.github.Chronox"

name := "ChronoxPool"

version := "0.1.0-SNAPSHOT"

scalaVersion := "2.12.4"

val port = 8080
containerPort in Jetty := port

resolvers += Classpaths.typesafeReleases

libraryDependencies ++= Seq(
  "org.scalatra" %% "scalatra" % ScalatraVersion,
  "org.scalatra" %% "scalatra-scalatest" % ScalatraVersion % "test",
  "com.typesafe.akka" %% "akka-http"   % "10.1.0-RC1", 
  "com.typesafe.akka" %% "akka-stream" % "2.5.8",
  "ch.qos.logback" % "logback-classic" % "1.2.3" % "runtime",
  "org.eclipse.jetty" % "jetty-webapp" % "9.4.8.v20171121" % "container",
  "javax.servlet" % "javax.servlet-api" % "3.1.0" % "provided",
  "net.liftweb" %% "lift-json" % "3.2.0-M3",
  "org.scalatra" %% "scalatra-json" % ScalatraVersion,
  "org.json4s"   %% "json4s-jackson" % "3.6.0-M2"
)

enablePlugins(SbtTwirl)
enablePlugins(ScalatraPlugin)
