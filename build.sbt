scalaVersion in ThisBuild := "2.12.2"

lazy val commonSettings = Seq(
  organization := "com.github.wildprairie",
  version := "0.1.0-SNAPSHOT",
  scalacOptions ++= Seq(
    "-deprecation",
    "-feature",
    "-language:existentials",
    "-language:postfixOps",
    "-language:higherKinds",
    "-language:implicitConversions",
    "-language:experimental.macros",
    "-unchecked",
//    "-Xfatal-warnings",
    "-Xlint",
    "-Yno-adapted-args",
    "-Ywarn-dead-code",
    "-Ywarn-value-discard",
    "-Xfuture",
    "-opt:l:classpath"
  )
)

lazy val protocol = Project(id = "core", base = file("dependencies/wakfutcp"))

lazy val common = (project in file("common"))
  .settings(commonSettings)
  .settings(
    moduleName := "wildprairie-common",
    libraryDependencies ++= Seq(
      "com.typesafe.akka" %% "akka-actor" % "2.5.1",
      "com.typesafe.akka" %% "akka-cluster" % "2.5.1"
    )
  ).dependsOn(protocol)

lazy val auth = (project in file("auth"))
  .settings(commonSettings)
  .settings(
    moduleName := "wildprairie-auth",
    libraryDependencies ++= Seq(
      "io.getquill" %% "quill-async-postgres" % "1.2.1"
    )
  )
  .dependsOn(common)


lazy val world = (project in file("world"))
  .settings(commonSettings)
  .settings(
    moduleName := "wildprairie-world",
    libraryDependencies ++= Seq(
      "com.typesafe.akka" %% "akka-persistence" % "2.5.1",
      "org.iq80.leveldb" % "leveldb" % "0.7",
      "org.fusesource.leveldbjni" % "leveldbjni-all" % "1.8"
    )
  )
  .dependsOn(common)


lazy val master = (project in file("master"))
  .settings(commonSettings)
  .settings(
    moduleName := "wildprairie-master"
  )
  .dependsOn(common)