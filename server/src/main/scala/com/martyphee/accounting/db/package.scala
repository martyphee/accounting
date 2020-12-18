package com.martyphee.accounting

import cats.effect.Resource
import skunk.Session
import zio._
import zio.console.Console
import zio.interop.catz._
import natchez.Trace.Implicits.noop

package object db {
  type DbSession = Has[DbSession.Service]
  implicit val runtime: Runtime[ZEnv] = Runtime.default

  object DbSession {
    trait Service {
      def session: Managed[Throwable, skunk.Session[Task]]
    }

    object DbPool {
      trait PoolConfig {}
    }

    val live: ZLayer[Console, Throwable, DbSession] = ZLayer.fromFunction { console =>
      new Service {
        def session: Managed[Throwable, skunk.Session[Task]] = {
          val session: Resource[Task, Session[Task]] = Session.single(
            host = "localhost",
            user = "postgres",
            database = "world",
            password = Some("postgres")
          )
          session.toManaged
        }
      }
    }


    def session: ZIO[DbSession, Throwable, Managed[Throwable, skunk.Session[Task]]] =
      ZIO.access(_.get.session)
  }
}
