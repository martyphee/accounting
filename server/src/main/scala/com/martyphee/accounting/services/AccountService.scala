package com.martyphee.accounting.services

import com.martyphee.accounting.repo.Account
import com.martyphee.accounting.repo.AccountRepoLayer.AccountRepo
import zio._
import zio.console._

import java.util.UUID
import zio.UIO
import zio.IO

object AccountServiceLayer {
    type AccountService = Has[AccountService.Service]

    object AccountService {
      trait Service {
        def find(id: Int): ZIO[Any, String, Account]
      }

      val live: ZLayer[Console with AccountRepo, String, AccountService] = ZLayer.fromFunction { console: Console => petId: Int =>
        console.get.putStrLn(s"Got request for pet: $petId") *> {
          if (petId == 35) {
            UIO(Account(UUID.randomUUID, "Tapirus terrestris"))
          } else {
            IO.fail("Unknown pet id")
          }
        }
      }

      def find(id: Int): ZIO[AccountService, String, Account] = ZIO.accessM(_.get.find(id))
    }
}
