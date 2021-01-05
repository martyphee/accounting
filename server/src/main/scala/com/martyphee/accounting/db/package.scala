package com.martyphee.accounting

import cats.effect._
import natchez.Trace
import skunk.Session
import zio._
import zio.interop.catz._
import skunk.util.Typer

package object db {
  type SessionPool = Has[SessionPool.Service]

  object SessionPool {
    trait Service {
      val session: ZManaged[Any, Throwable, Session[Task]]
    }

    def live(
              implicit concurrent: Concurrent[Task],
              contextShift: ContextShift[Task],
              trace: Trace[Task]
            ): ZLayer[config.DBConfigService, Throwable, SessionPool] =
      ZLayer.fromServiceManaged { config =>
        Session
          .pooled[Task](
            host = config.host,
            port = config.port,
            user = config.user,
            database = config.database,
            password = config.password,
            max = 4,
            strategy = Typer.Strategy.SearchPath,
          )
          .toManagedZIO
          .map(
            resource =>
              new SessionPool.Service {
                val session = resource.toManagedZIO
              }
          )
      }
  }

  val session: ZManaged[SessionPool, Throwable, Session[Task]] =
    ZManaged.accessManaged(_.get.session)
}
