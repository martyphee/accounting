import Dependencies._
import Versions.zioLogging

ThisBuild / scalaVersion     := "2.13.3"
ThisBuild / version          := "0.1.0-SNAPSHOT"
ThisBuild / organization     := "com.example"
ThisBuild / organizationName := "example"

lazy val flywaySettings = Seq(
  flywayUrl := "jdbc:postgresql://localhost:5432/accounting",
  flywayUser := "postgres",
  flywayPassword := "",
  flywayUrl in Test := "jdbc:postgresql://localhost:5432/accounting_test",
  flywayUser in Test := "postgres",
  flywayPassword in Test := "",
  flywayBaselineOnMigrate := true
)

val commonJvmSettings: Seq[Def.Setting[_]] = commonSmlBuildSettings

lazy val dbDependencies = List(
  "org.tpolecat" %% "skunk-core" % "0.0.20"
)

lazy val root = (project in file("server"))
  .settings(commonJvmSettings)
  .settings(
    name := "accounting",
    libraryDependencies ++= dbDependencies ++ Seq(
      scalaTest % Test,
      "dev.zio" %% "zio-interop-cats" % Versions.zioInteropCats,
      "org.typelevel" %% "cats-effect" % Versions.catsEffect,
      "dev.zio"                      %% "zio-logging"         % zioLogging,
      "dev.zio"                      %% "zio-logging-slf4j"   % zioLogging,
      "org.apache.logging.log4j"      % "log4j-api"           % Versions.log4j,
      "org.apache.logging.log4j"      % "log4j-core"          % Versions.log4j,
      "org.apache.logging.log4j"      % "log4j-slf4j-impl"    % Versions.log4j,
    )
  )

lazy val migrations = project.in(file("migrations"))
  .enablePlugins(FlywayPlugin)
  .settings(flywaySettings: _*)
  .settings {
    flywayLocations += "db/migrations"
    libraryDependencies ++= Seq(
      "org.postgresql" % "postgresql" % "42.2.5"
    )
  }
