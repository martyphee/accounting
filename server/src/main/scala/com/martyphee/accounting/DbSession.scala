package com.martyphee.accounting

import cats.effect.Resource
import natchez.Trace.Implicits.noop
import skunk._
import zio.interop.catz._
import zio.{Has, RIO, Runtime, Task, TaskManaged, UIO, ZEnv, ZIO, ZLayer}

object DbSessionLayer {
  type DbSession = Has[DbSession.Service]
  implicit val runtime: Runtime[ZEnv] = Runtime.default

  object DbSession {
    trait Service {
      def session: RIO[DbSession, TaskManaged[Session[Task]]]
    }

    val live: ZLayer[Any, Throwable, DbSession] = ZLayer.fromFunction { _ =>
      new Service {
        override def session: UIO[TaskManaged[Session[Task]]] =
          ZIO.runtime[Any].map { implicit rts =>
            val s: Resource[Task, Session[Task]] = Session.single(
              host = "localhost",
              user = "postgres",
              database = "world",
              password = Some("postgres")
            )

            s.toManaged
          }
      }
    }

    def session: RIO[DbSession, TaskManaged[Session[Task]]] =
      ZIO.accessM(_.get.session)
  }
}
