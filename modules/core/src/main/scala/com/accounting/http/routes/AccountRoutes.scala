package com.accounting.http.routes

import cats._
import org.http4s._
import org.http4s.circe.JsonDecoder
import org.http4s.dsl.Http4sDsl
import org.http4s.server.Router
import com.accounting.algebras.Accounts
import com.accounting.domain.account._
import com.accounting.effects.MonadThrow
import com.accounting.http.routes.params._
import com.accounting.http.routes.decoder._
import com.accounting.http.routes.json._

final class AccountRoutes[F[_]: Defer: JsonDecoder: MonadThrow](
    accounts: Accounts[F]
) extends Http4sDsl[F] {

  private[routes] val prefixPath = "/accounts"

  object AccountIdQueryParam extends QueryParamDecoderMatcher[AccountIdParm]("account")

  private val httpRoutes: HttpRoutes[F] = HttpRoutes.of[F] {
    case GET -> Root =>
      Ok(accounts.getN())

    case GET -> Root / UUIDVar(accountId) =>
      Ok(accounts.get(AccountId(accountId)))

    case req @ POST -> Root =>
      req.decodeR[AccountCreateRequest] { accountRequest =>
        Ok(accounts.create(accountRequest.name))
      }
  }

  val routes: HttpRoutes[F] = Router(
    prefixPath -> httpRoutes
  )
}
