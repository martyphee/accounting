package com.martyphee.accounting.repo

import zio.console.Console
import zio.{Has, IO, UIO, ZIO, ZLayer}

import java.util.UUID

case class Account(id: UUID, name: String)

object AccountRepoLayer {
  type AccountRepo = Has[AccountRepo.Service]

  object AccountRepo {
    trait Service {
      def find(id: String): ZIO[Any, String, Account]
    }

    val live: ZLayer[Console, String, AccountRepo] = ZLayer.fromFunction { console: zio.console.Console => accountId: String =>
      console.get.putStrLn(s"Got request for pet: $accountId") *> {
        if (accountId == "42") {
          UIO(Account(UUID.randomUUID, "Tapirus terrestris"))
        } else {
          IO.fail("Unknown pet id")
        }
      }
    }

    def find(id: String): ZIO[AccountRepo, String, Account] = ZIO.accessM(_.get.find(id))
  }
}
