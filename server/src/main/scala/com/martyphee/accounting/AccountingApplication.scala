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

object BasicPersistence extends LazyLogging {
  type BasicPersistence = Has[BasicPersistence.Service]

  trait Service {
    def doBasic(session: Session[Task]): Task[OffsetDateTime]
  }

  val extended: Query[String, Country] =
    sql"""
      SELECT name, code, population
      FROM   country
      WHERE  name like $text
    """
      .query(varchar ~ bpchar(3) ~ int4)
      .gmap[Country]

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

object AccountingApplication extends App with LazyLogging {
  import com.martyphee.accounting.BasicPersistence.BasicPersistence

  override def run(args: List[String]): URIO[ZEnv, ZExitCode] = {
    type AppEnvironment = Console with DbSession with BasicPersistence
    val appEnvironment =
      zio.console.Console.live >+> DbSession.live >+> BasicPersistence.live

    val program: ZIO[AppEnvironment, Throwable, Unit] = {
      for {
        _ <- putStrLnErr("Application is starting up")
        db <- ZIO.service[DbSession.Service]
        _ <- db.session.use({ s =>
          for {
            result <- BasicPersistence.doBasic(s)
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
