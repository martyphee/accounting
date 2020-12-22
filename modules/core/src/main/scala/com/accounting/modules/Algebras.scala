package com.accounting.modules

import cats.effect._
import cats.syntax.all._
import cats.effect.Resource
import com.accounting.algebras._
import skunk._

object Algebras {
  def make[F[_]: Sync](
      sessionPool: Resource[F, Session[F]]
  ): F[Algebras[F]] =
    for {
      accounts <- LiveAccounts.make[F](sessionPool)
    } yield new Algebras[F](accounts)
}

final class Algebras[F[_]] private (
  val account: Accounts[F]
)
