val akkaV = "2.6.14"

scalaVersion := "2.13.1"

organization := "com.example"
name := "distributed-word-counter"

libraryDependencies ++= Seq(
  "com.typesafe.akka" %% "akka-actor" % akkaV,
  "com.typesafe.akka" %% "akka-remote" % akkaV,
  "com.typesafe.akka" %% "akka-cluster" % akkaV,
  "com.typesafe.akka" %% "akka-cluster-sharding" % akkaV,
  "com.typesafe.akka" %% "akka-cluster-tools" % akkaV,
  "io.aeron" % "aeron-driver" % "1.32.0",
  "io.aeron" % "aeron-client" % "1.32.0"
)