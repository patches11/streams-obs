ThisBuild / version := "0.1.0-SNAPSHOT"

ThisBuild / scalaVersion := "2.11.0"

val akkaVersion = "2.5.32"

lazy val root = (project in file("."))
  .settings(
    name := "streams-obs",
    resourceDirectory := baseDirectory.value / "src/main/resources",
    libraryDependencies := Seq(
      "org.slf4j" % "slf4j-log4j12" % "1.7.30",
      "com.typesafe.akka" %% "akka-actor" % akkaVersion,
      "com.typesafe.akka" %% "akka-stream" % akkaVersion,
      "com.typesafe.akka" %% "akka-slf4j" % akkaVersion,
    )
  )


