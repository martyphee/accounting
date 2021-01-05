package com.martyphee.accounting

import com.martyphee.accounting.db._
import com.typesafe.scalalogging.LazyLogging
import natchez.Trace.Implicits.noop
import com.martyphee.accounting.config.DBConfig
import skunk.Query
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
    def doBasic: Task[OffsetDateTime]
  }

  val live: ZLayer[SessionPool, Throwable, BasicPersistence] = ZLayer.fromService { db: SessionPool.Service =>
    new Service {
      override def doBasic: Task[OffsetDateTime] = {
        db.session.use { session =>
          session.unique(sql"select current_timestamp".query(timestamptz))
        }
      }
    }
  }

  def doBasic: ZIO[BasicPersistence, Throwable, OffsetDateTime] = ZIO.accessM(
    _.get.doBasic
  )
}

object ExtendedPersistence {
  type ExtendedPersistence = Has[ExtendedPersistence.Service]

  trait Service {
    def doExtended: Task[List[Country]]
  }

  val extended: Query[String, Country] =
    sql"""
      SELECT name, code, population
      FROM   country
      WHERE  name like $text
    """
      .query(varchar ~ bpchar(3) ~ int4)
      .gmap[Country]

  val live: ZLayer[SessionPool, Nothing, ExtendedPersistence] = ZLayer.fromService { db: SessionPool.Service =>
    new Service {
      override def doExtended: Task[List[Country]] = {
        db.session.use { session => {
          session.prepare(extended).use { pq =>
            pq.stream("U%", 32).compile.toList
          }
        }}
      }
    }
  }

  def doExtended(): ZIO[ExtendedPersistence, Throwable, List[Country]] = ZIO.accessM(
    _.get.doExtended
  )
}

object AccountingApplication extends App with LazyLogging {
  import com.martyphee.accounting.BasicPersistence.BasicPersistence
  import com.martyphee.accounting.ExtendedPersistence.ExtendedPersistence

  override def run(args: List[String]): URIO[ZEnv, ZExitCode] = {
    type AppEnvironment = Console with SessionPool with BasicPersistence with ExtendedPersistence

    val appEnvironment =
      zio.console.Console.live >+> DBConfig.live >+> SessionPool.live >+>
        BasicPersistence.live >+> ExtendedPersistence.live

    val program: ZIO[AppEnvironment, Throwable, Unit] = {
      for {
        _ <- putStrLnErr("Application is starting up")
        result <- BasicPersistence.doBasic
        _ <- putStrErr(s"The time is $result")
        countries <- ExtendedPersistence.doExtended()
        _ <- putStrErr(s"Found countries $countries")
      } yield (ZExitCode.failure)
    }

    program
      .provideSomeLayer[ZEnv](appEnvironment)
      .tapError(err => putStrLn(s"We had an error $err"))
      .exitCode
  }
}
