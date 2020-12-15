package com.martyphee.accounting

import fs2.Stream
import zio.{Has, RIO, Task}

package object repo {

  object Repo {
    trait Service[A] {
      def find(id: String): Task[Stream[Task, Account]]
    }
  }

  type AccountRepo = Has[Repo.Service[Account]]

  def find(id: String): RIO[AccountRepo, Stream[Task, Account]] = RIO.accessM(_.get.find(id))
}
