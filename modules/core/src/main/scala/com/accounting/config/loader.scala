package com.accounting.config

import cats.effect._
import cats.syntax.all._
import ciris._
import ciris.refined._
import environments._
import environments.AppEnvironment._
import eu.timepit.refined.auto._

import com.accounting.config.data._
import eu.timepit.refined.cats._
import eu.timepit.refined.types.string.NonEmptyString

import scala.concurrent.duration._

object load {

  // Ciris promotes configuration as code
  def apply[F[_]: Async: ContextShift]: F[AppConfig] =
    env("SC_APP_ENV")
      .as[AppEnvironment]
      .flatMap {
        case Test =>
          default(
            redisUri = RedisURI("redis://localhost")
          )
        case Prod =>
          default(
            redisUri = RedisURI("redis://10.123.154.176")
          )
      }
      .load[F]

  private def default(
      redisUri: RedisURI
  ): ConfigValue[AppConfig] =
    (
      env("SC_JWT_SECRET_KEY").as[NonEmptyString].secret,
      env("SC_JWT_CLAIM").as[NonEmptyString].secret
    ).parMapN { (_, _) =>
      AppConfig(
        HttpClientConfig(
          connectTimeout = 2.seconds,
          requestTimeout = 2.seconds
        ),
        PostgreSQLConfig(
          host = "localhost",
          port = 5432,
          user = "postgres",
          database = "accounting",
          max = 10
        ),
        RedisConfig(redisUri),
        HttpServerConfig(
          host = "0.0.0.0",
          port = 8080
        )
      )
    }
}
