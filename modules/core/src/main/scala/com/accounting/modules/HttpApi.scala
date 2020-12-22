package com.accounting.modules

import cats.effect._
import com.accounting.http.routes.{AccountRoutes, version}
import org.http4s._
import org.http4s.implicits._
import org.http4s.server.Router

object HttpApi {
  def make[F[_]: Concurrent: Timer](
    algebras: Algebras[F]
  ): F[HttpApi[F]] =
    Sync[F].delay(
      new HttpApi[F](algebras)
    )
}

final class HttpApi[F[_]: Concurrent: Timer] private (
  algebras: Algebras[F]
) {
  private val accountRoutes = new AccountRoutes[F](algebras.account).routes

  private val openRoutes: HttpRoutes[F] = accountRoutes

  private val routes: HttpRoutes[F] = Router(
    version.v1 -> openRoutes,
  )

  val httpApp: HttpApp[F] = routes.orNotFound
}
