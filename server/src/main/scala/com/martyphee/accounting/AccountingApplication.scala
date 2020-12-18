package com.martyphee.accounting

import cats.effect.Resource
import com.martyphee.accounting.ExtendedPersistence.ExtendedPersistence
import com.martyphee.accounting.db.DbSession
import com.typesafe.scalalogging.LazyLogging
import skunk._
import skunk.implicits._
import skunk.codec.all._
import zio.{ExitCode => ZExitCode, _}
import zio.console._
import zio.interop.catz._

import java.time.OffsetDateTime

// a data model
case class Country(name: String, code: String, population: Int)

object BasicPersistence extends LazyLogging {
  type BasicPersistence = Has[BasicPersistence.Service]

  trait Service {
    def doBasic(session: Session[Task]): Task[OffsetDateTime]
  }

  val live: ZLayer[DbSession, Nothing, BasicPersistence] = ZLayer.fromService { db: DbSession.Service =>
      new Service {
        override def doBasic(session: Session[Task]): Task[OffsetDateTime] = {
          session.unique(sql"select current_timestamp".query(timestamptz))
        }
      }
  }

  def doBasic(session: Session[Task]): URIO[BasicPersistence, Task[OffsetDateTime]] = ZIO.access(
    _.get.doBasic(session)
  )
}

object ExtendedPersistence {
  type ExtendedPersistence = Has[ExtendedPersistence.Service]

  trait Service {
    def doExtended(session: Session[Task]): Resource[Task, fs2.Stream[Task, Country]]
  }

  val extended: Query[String, Country] =
    sql"""
      SELECT name, code, population
      FROM   country
      WHERE  name like $text
    """
      .query(varchar ~ bpchar(3) ~ int4)
      .gmap[Country]

  val live: ZLayer[DbSession, Nothing, ExtendedPersistence] = ZLayer.fromService { db: DbSession.Service =>
    new Service {
      override def doExtended(session: Session[Task]): Resource[Task, fs2.Stream[Task, Country]] = {
        session.prepare(extended).map { pq =>
          pq.stream("U%", 32)
        }
      }
    }
  }

  def doExtended(session: Session[Task]): URIO[ExtendedPersistence, Resource[Task, fs2.Stream[Task, Country]]] = ZIO.access(
    _.get.doExtended(session)
  )
}

object AccountingApplication extends App with LazyLogging {
  import com.martyphee.accounting.BasicPersistence.BasicPersistence
  import com.martyphee.accounting.ExtendedPersistence.ExtendedPersistence

  override def run(args: List[String]): URIO[ZEnv, ZExitCode] = {
    type AppEnvironment = Console with DbSession with BasicPersistence with ExtendedPersistence
    val appEnvironment =
      zio.console.Console.live >+> DbSession.live >+> BasicPersistence.live >+> ExtendedPersistence.live

    val program: ZIO[AppEnvironment, Throwable, Unit] = {
      for {
        _ <- putStrLnErr("Application is starting up")
        db <- ZIO.service[DbSession.Service]
        _ <- db.session.use({ s =>
          for {
            result <- BasicPersistence.doBasic(s)
            t <- result
            _ <- putStrLn(s"Execution done: ${t}")
            result2 <- ExtendedPersistence.doExtended(s)
            _ <- result2.use({ q =>
              q.evalMap(c => putStrLn(s"${c}"))
                .compile
                .drain
            })

          } yield ()
        })
      } yield (ZExitCode.failure)
    }

    program
      .provideSomeLayer[ZEnv](appEnvironment)
      .tapError(err => putStrLn(s"We had an error $err"))
      .exitCode
  }
}
