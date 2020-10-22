name := "dag-relay-service"

version := "1.0"

scalaVersion := "2.13.3"

val zioVersion = "1.0.3"
val circeVersion = "0.12.3"
val http4sVersion = "0.21.8"

// Only necessary for SNAPSHOT releases
resolvers += Resolver.sonatypeRepo("snapshots")

libraryDependencies ++= Seq(
  "io.circe" %% "circe-core",
  "io.circe" %% "circe-generic",
  "io.circe" %% "circe-parser"
).map(_ % circeVersion)

libraryDependencies ++= Seq(
  "dev.zio" %% "zio" % zioVersion,
  "dev.zio" %% "zio-interop-cats" % "2.2.0.1",

  "org.http4s" %% "http4s-dsl" % http4sVersion,
  "org.http4s" %% "http4s-blaze-server" % http4sVersion,
  //"org.http4s" %% "http4s-blaze-client" % http4sVersion,
  //"org.http4s" %% "http4s-circe" % http4sVersion,
  // Optional for string interpolation to JSON model

  "org.slf4j" % "slf4j-simple" % "1.7.22",
  "org.slf4j" % "slf4j-api" % "1.7.22"

  //"org.scala-lang" % "scala-reflect" % scalaVersion.value ,
  //"com.github.pureconfig" %% "pureconfig" % "0.12.3"
)