name := "scala-leaderboard"

version := "2.0.5"

organization := "org.agoragames"

scalaVersion := "2.11.7"

libraryDependencies += "net.debasishg" %% "redisclient" % "3.1.1"

libraryDependencies += "junit" % "junit" % "4.8.1" % "test"
 
libraryDependencies += "org.scalatest" %% "scalatest" % "3.0.0-M7" % "test"

scalacOptions ++= Seq("-deprecation")
