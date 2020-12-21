package com.accounting.http.routes

import cats._
import com.accounting.algebras.Accounts
import com.accounting.domain.account._
import org.http4s._
import org.http4s.dsl.Http4sDsl
import org.http4s.server.Router
import com.accounting.http.routes.params._
import com.accounting.http.routes.json._

final class AccountRoutes[F[_]: Defer: Monad](
    accounts: Accounts[F]
) extends Http4sDsl[F] {

  private[routes] val prefixPath = "/accounts"

  object AccountIdQueryParam extends QueryParamDecoderMatcher[AccountIdParm]("account")

  private val httpRoutes: HttpRoutes[F] = HttpRoutes.of[F] {

    case GET -> Root :? AccountIdQueryParam(accountId) =>
      Ok(accounts.get(accountId.toDomain))

  }

  val routes: HttpRoutes[F] = Router(
    prefixPath -> httpRoutes
  )

}
