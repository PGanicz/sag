name := "sag"
organization in ThisBuild := "org.sag"
scalaVersion in ThisBuild := "2.12.1"

resolvers += "scalaz-bintray" at "https://dl.bintray.com/scalaz/releases"

resolvers += "Akka Snapshot Repository" at "https://repo.akka.io/snapshots/"

lazy val global = (project in file("."))
  .aggregate(
    remoteapi,
    simulation,
    display
  )

lazy val remoteapi = project
  .settings(libraryDependencies += "com.typesafe.akka" %% "akka-actor" % "2.6.1")
lazy val simulation = project
  .dependsOn(remoteapi)
  .settings(libraryDependencies ++= Seq(
    "com.typesafe.akka" %% "akka-actor" % "2.6.1",
    "com.typesafe.akka" %% "akka-cluster-tools" % "2.6.1",
    "com.typesafe.akka"     %% "akka-remote" % "2.6.1",
    "com.typesafe.akka"     %% "akka-persistence" % "2.6.1"
  ) )
lazy val display = (project in file("display"))
  .dependsOn(remoteapi)
  .enablePlugins(PlayScala)
  .settings(libraryDependencies ++= Seq("com.typesafe.akka" %% "akka-actor" % "2.6.1",
    "com.typesafe.akka" %% "akka-cluster-tools" % "2.6.1",
    "com.typesafe.akka"     %% "akka-remote" % "2.6.1",
     jdbc , ehcache , ws , specs2 % Test , guice,
    "com.typesafe.akka" %% "akka-cluster-typed" % "2.6.1",
    "com.typesafe.play" %% "play-iteratees" % "2.6.1") )

