package com.martyphee.accounting

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

object Persistence extends LazyLogging {
  type Persistence = Has[Persistence.Service]

  trait Service {
    def doExtended(session: Session[Task]): Task[OffsetDateTime]
  }

  val extended: Query[String, Country] =
    sql"""
      SELECT name, code, population
      FROM   country
      WHERE  name like $text
    """
      .query(varchar ~ bpchar(3) ~ int4)
      .gmap[Country]

  val live: ZLayer[DbSession, Nothing, Persistence] = ZLayer.fromService { db: DbSession.Service =>
      new Service {
        override def doExtended(session: Session[Task]): Task[OffsetDateTime] = {
          logger.debug("doExtended")
//          val prepared = session.prepare(extended)
//          prepared.use { ps =>
//            ps.stream("U%", 64)
//              .evalMap(c => IO(putStrLn(c.name)))
//              .compile
//              .drain
//          }
          session.unique(sql"select current_timestamp".query(timestamptz))
        }
      }
  }

  def doExtended(session: Session[Task]): URIO[Persistence, Task[OffsetDateTime]] = ZIO.access(
    _.get.doExtended(session)
  )
}

object AccountingApplication extends App with LazyLogging {
  import com.martyphee.accounting.Persistence.Persistence

  override def run(args: List[String]): URIO[ZEnv, ZExitCode] = {
    type AppEnvironment = Console with DbSession with Persistence
    val appEnvironment =
      zio.console.Console.live >+> DbSession.live >+> Persistence.live

    val program: ZIO[AppEnvironment, Throwable, Unit] = {
      for {
        _ <- putStrLnErr("Application is starting up")
        db <- ZIO.service[DbSession.Service]
        _ <- db.session.use({ s =>
          for {
            result <- Persistence.doExtended(s)
            t <- result
            _ <- putStrLn(s"Execution done: ${t}")
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
