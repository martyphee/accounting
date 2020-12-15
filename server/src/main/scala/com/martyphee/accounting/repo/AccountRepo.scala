package com.martyphee.accounting.repo

import zio._
import zio.interop.catz._
import com.martyphee.accounting.DbSessionLayer.DbSession
import skunk._
import skunk.implicits._
import skunk.codec.all._
import fs2.Stream

import java.util.UUID

case class Account(id: UUID, name: String)

final class AccountRepoService(session: TaskManaged[Session[Task]]) extends Repo.Service[Account] {
  import AccountRepoService._

  implicit val runtime: Runtime[ZEnv] = Runtime.default

  def find(id: String): Task[Stream[Task, Account]] = {
    session.use { s =>
      UIO(for {
        ps <- Stream.resource(s.prepare(SQL.find))
        c <- ps.stream("test", 32)
      } yield (c))
    }
  }
}

object AccountRepoService {
  object SQL {
    def find: Query[String, Account] =
      sql"""
            select id, name
            from account
            where id = $text
           """
        .query(uuid ~ varchar)
        .gmap[Account]
  }

  val live: ZLayer[DbSession, Throwable, AccountRepo] =
    ZLayer.fromService(new AccountRepoService(_))
}
