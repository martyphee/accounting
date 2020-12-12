package com.martyphee.accounting.services

import zio.ZIO
import zio.Has
import zio.ZLayer
import java.util.UUID
import zio.UIO
import zio.IO

case class Account(id: UUID, name: String, url: String)

object AccountServiceLayer {
    type AccountService = Has[AccountService.Service]

    object AccountService {
      trait Service {
        def find(id: Int): ZIO[Any, String, Account]
      }

      val live: ZLayer[zio.console.Console, String, AccountService] = ZLayer.fromFunction { console: zio.console.Console => petId: Int =>
        console.get.putStrLn(s"Got request for pet: $petId") *> {
          if (petId == 35) {
            UIO(Account(UUID.randomUUID, "Tapirus terrestris", "https://en.wikipedia.org/wiki/Tapir"))
          } else {
            IO.fail("Unknown pet id")
          }
        }
      }

      def find(id: Int): ZIO[AccountService, String, Account] = ZIO.accessM(_.get.find(id))
    }
}