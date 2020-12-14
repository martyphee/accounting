package com.martyphee.accounting

import natchez.Trace.Implicits.noop
import cats.effect.Resource
import zio.UIO
import skunk._
import zio.{Has, Task, ZIO, ZLayer}
import zio.interop.catz._

object DbSessionLayer {
  type DbSession = Has[DbSession.Service]

  object DbSession {
    trait Service {
      def session: ZIO[Any, Throwable, Resource[Task, Session[Task]]]
    }

    val live: ZLayer[Any, Throwable, DbSession] = ZLayer.fromFunction { _ =>
      new Service {
        override def session
            : ZIO[Any, Throwable, Resource[Task, Session[Task]]] =
          UIO(Session.single(
            host = "localhost",
            user = "postgres",
            database = "world",
            password = Some("postgres")
          ))
      }
    }

    def session: ZIO[DbSession, Throwable, Resource[Task, Session[Task]]] =
      ZIO.accessM(_.get.session)
  }
}
