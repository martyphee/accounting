package com.martyphee.accounting.services

import com.martyphee.accounting.repo._
import zio._
import zio.console._
import zio.UIO
import zio.IO

object AccountServiceLayer {
  type AccountService = Has[AccountService.Service]

  object AccountService {
    trait Service {
      def find(id: Int): ZIO[AccountService, Throwable, Account]
    }

    val live: ZLayer[Console with AccountRepo, Throwable, AccountService] =
//      ZLayer.fromFunction { console: Console => petId: Int =>
//        console.get.putStrLn(s"Got request for pet: $petId") *> {
//          if (petId == 35) {
//            find(petId.toString).flatMap { a =>
//              UIO(a)
//            }
//          } else {
//            IO.fail(new Exception("Unknown pet id"))
//          }
//        }
//      }
      ZLayer.fromFunction(console =>
        new Service {
          override def find(
              petId: Int
          ): ZIO[AccountService, Throwable, Account] =
            console.get.putStrLn(s"Got request for pet: $petId") *> {
              if (petId == 35) {
                find(petId).flatMap { a =>
                  UIO(a)
                }
              } else {
                IO.fail(new Exception("Unknown pet id"))
              }
            }
        }
      )
  }
}
