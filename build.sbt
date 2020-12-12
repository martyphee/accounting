import Dependencies._

ThisBuild / scalaVersion     := "2.13.3"
ThisBuild / version          := "0.1.0-SNAPSHOT"
ThisBuild / organization     := "com.example"
ThisBuild / organizationName := "example"

lazy val loggerDependencies = Seq(
  "ch.qos.logback" % "logback-classic" % "1.2.3",
  "ch.qos.logback" % "logback-core" % "1.2.3",
  "com.typesafe.scala-logging" %% "scala-logging" % "3.9.2"
)

val commonJvmSettings: Seq[Def.Setting[_]] = commonSmlBuildSettings

lazy val root = (project in file("."))
  .settings(commonJvmSettings)
  .settings(
    name := "accounting",
    libraryDependencies ++= loggerDependencies ++ Seq(
      scalaTest % Test,
      "dev.zio" %% "zio-interop-cats" % Versions.zioInteropCats,
      "io.circe" %% "circe-core" % Versions.circe,
      "io.circe" %% "circe-parser" % Versions.circe,
      "io.circe" %% "circe-generic-extras" % Versions.circe,
      "org.typelevel" %% "cats-effect" % Versions.catsEffect,
      "org.http4s" %% "http4s-dsl" % Versions.http4s,
      "com.softwaremill.sttp.tapir" %% "tapir-zio" % "0.17.0-M10",
      "com.softwaremill.sttp.tapir" %% "tapir-zio-http4s-server" % "0.17.0-M10",
      "com.softwaremill.sttp.tapir" %% "tapir-json-circe" % "0.17.0-M10",
      "com.softwaremill.sttp.tapir" %% "tapir-swagger-ui-http4s" % "0.17.0-M10",
      "com.softwaremill.sttp.tapir" %% "tapir-openapi-docs" % "0.17.0-M10",
      "com.softwaremill.sttp.tapir" %% "tapir-openapi-circe-yaml" % "0.17.0-M10"
    )
  )

// See https://www.scala-sbt.org/1.x/docs/Using-Sonatype.html for instructions on how to publish to Sonatype.
