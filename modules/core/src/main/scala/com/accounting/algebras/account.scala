package com.accounting.algebras

import cats.effect._
import cats.syntax.all._
import com.accounting.domain.account._
import com.accounting.effects.GenUUID
import com.accounting.ext.skunkx._
import skunk._
import skunk.codec.all._
import skunk.implicits._


trait Accounts[F[_]] {
  def get(accountId: AccountId): F[Option[Account]]

  def create(
      name: AccountName
  ): F[AccountId]
}


object LiveAccounts {
  def make[F[_]: Sync](
    sessionPool: Resource[F, Session[F]]
  ): F[Accounts[F]] =
    Sync[F].delay(
      new LiveAccounts[F](sessionPool)
    )
}

private class LiveAccounts[F[_]: Sync](
    sessionPool: Resource[F, Session[F]]
) extends Accounts[F] {
  import AccountQueries._

  override def get(accountId: AccountId): F[Option[Account]] = {
    sessionPool.use { session =>
      session.prepare(selectAccountById).use { ps =>
        ps.option(accountId)
      }
    }
  }

  override def create(name: AccountName): F[AccountId] = {
    sessionPool.use { session =>
      session.prepare(createAccount).use { ps =>
        GenUUID[F].make[AccountId].flatMap { id =>
          ps
            .execute(AccountCreate(id, name))
            .as(id)
        }
      }
    }
  }
}

private object AccountQueries {
  val decoder: Decoder[Account] =
    (
      uuid.cimap[AccountId] ~
        varchar(255).cimap[AccountName] ~
        timestamp.cimap[CreatedAt] ~
        timestamp.cimap[UpdatedAt]
      ).map {
      case o ~ n ~ t ~ u =>
        Account(o, n, t, u)
    }

  val createCodec: Codec[AccountCreate] =
    (
      uuid.cimap[AccountId] ~
        varchar(150).cimap[AccountName]
      ).imap {
      case i ~ n => AccountCreate(i, n)
    }(a => a.uuid ~ a.name)

  val selectAccountById: Query[AccountId, Account] =
    sql"""
         select *
         from   account
         where id = ${uuid.cimap[AccountId]}
    """.query(decoder)

  val createAccount: Command[AccountCreate] =
    sql"""
         insert into account
         values($createCodec)
       """.command
}
