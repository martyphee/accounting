package com.martyphee.accounting

import com.martyphee.accounting.db.DbSession
import skunk._
import skunk.implicits._
import skunk.codec.all._
import zio._
import zio.console.{Console, putStrLn, putStrLnErr}
import zio.interop.catz._
import fs2._
import natchez.Trace.Implicits.noop
import zio.logging.Logging
import zio.logging.slf4j.Slf4jLogger

// a data model
case class Country(name: String, code: String, population: Int)

object Persistence {
  type Persistence = Has[Persistence.Service]

  import org.slf4j.Logger
  import org.slf4j.LoggerFactory

  val logger: Logger = LoggerFactory.getLogger(classOf[Nothing])

  trait Service {
    def doExtended(session: Session[Task]): Task[Unit]
  }

  val extended: Query[String, Country] =
    sql"""
      SELECT name, code, population
      FROM   country
      WHERE  name like $varchar
    """
      .query(varchar ~ bpchar(3) ~ int4)
      .gmap[Country]

  val live: ZLayer[DbSession, Nothing, Persistence] = ZLayer.fromService { db: DbSession.Service =>
      new Service {
        override def doExtended(session: Session[Task]): Task[Unit] = {
          logger.debug("doExtended")
//          val stream: Stream[Task, Unit] =
//            for {
//              ps <- Stream.resource(session.prepare(extended))
//              c <- ps.stream("U%", 64)
//              _ <- Stream.eval(IO(putStrLn(c.name)))
//            } yield ()
//          stream.compile.drain

          session.prepare(extended).use { ps =>
            ps.stream("U%", 64)
              .evalMap(c => IO(putStrLn(c.name)))
              .compile
              .drain
          }.tapError(err =>
            logging.log.error((s"Unable to prepare query $err"))
          ).catchAll( err =>
            logging.log.error(s"Unable to prepare query $err")
          )
          Task(logger.debug("doExtended End"))
        }
      }
  }

  def doExtended(session: Session[Task]): URIO[Persistence, Unit] = ZIO.access(
    _.get.doExtended(session)
  )
}

object AccountingApplication extends App {
  import com.martyphee.accounting.Persistence.Persistence

  override def run(args: List[String]): URIO[ZEnv, ExitCode] = {
    type AppEnvironment = Console with Logging with DbSession with Persistence
    val appEnvironment =
      zio.console.Console.live >+> Slf4jLogger.make((_, msg) =>
        msg
      ) >+> DbSession.live >+> Persistence.live

    val program: ZIO[AppEnvironment, Throwable, Unit] = {
      for {
        _ <- logging.log.info(s"Starting with test")
        _ <- putStrLn("Application is starting up")
        db <- ZIO.service[DbSession.Service]
        _ <- db.session.use({ s => Persistence.doExtended(s) })
      } yield (ExitCode.failure)
    }

    program
      .provideSomeLayer[ZEnv](appEnvironment)
      .tapError(err => putStrLn(s"We had an error $err"))
      .exitCode
  }
}
