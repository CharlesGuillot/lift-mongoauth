name := "lift-auth-mongo"

version := "2.4-SNAPSHOT-0.1"

organization := "com.eltimn"

scalaVersion := "2.9.1"

crossScalaVersions := Seq("2.9.1", "2.9.0-1", "2.9.0", "2.8.2", "2.8.1", "2.8.0")

resolvers += ScalaToolsSnapshots

libraryDependencies <++= (scalaVersion) { scalaVersion =>
  val scalatestVersion = scalaVersion match {
    case "2.8.0" => "1.3"
    case "2.8.1" | "2.8.2" => "1.5.1"
    case _       => "1.6.1"
  }
  val liftVersion = "2.4-SNAPSHOT"
  Seq(
    "net.liftweb" %% "lift-mongodb-record" % liftVersion % "compile",
    "ch.qos.logback" % "logback-classic" % "0.9.26" % "provided",
    "org.scalatest" %% "scalatest" % scalatestVersion % "test",
    "org.mindrot" % "jbcrypt" % "0.3m" % "compile"
  )
}

scalacOptions ++= Seq("-deprecation", "-unchecked")

//defaultExcludes ~= (_ || "*~")
