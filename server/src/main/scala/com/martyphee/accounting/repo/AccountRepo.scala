package com.martyphee.accounting.repo

import com.martyphee.accounting.DbSessionLayer.DbSession
import skunk._
import skunk.implicits._
import skunk.codec.all._
import zio.console.Console
import zio._
import zio.interop.catz._

import java.util.UUID

case class Account(id: UUID, name: String)

object AccountRepoLayer {
  type AccountRepo = Has[AccountRepo.Service]

  object AccountRepo {
    trait Service {
      def find(id: String): ZIO[Any, String, Account]
    }

    val live: ZLayer[Console with DbSession, String, AccountRepo] = ZLayer.fromFunction { console: zio.console.Console =>accountId: String =>
      console.get.putStrLn(s"Got request for pet: $accountId") *> {
        DbSession.session.flatMap { s =>
          s.use { session =>
            for {
              tz <- session.unique(sql"select current_timestamp".query(timestamptz))
            } yield UIO(Account(UUID.randomUUID, "Tapirus terrestris"))
          }
        }
//        if (accountId == "42") {
//          UIO(Account(UUID.randomUUID, "Tapirus terrestris"))
//        } else {
//          IO.fail("Unknown pet id")
//        }
      }
    }

    def find(id: String): ZIO[AccountRepo, String, Account] = ZIO.accessM(_.get.find(id))
  }
}
