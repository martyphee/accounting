package com.martyphee.accounting

import cats.syntax.all._
import com.martyphee.accounting.DbSessionLayer.DbSession
import com.martyphee.accounting.repo.{Account, AccountRepo}
import com.martyphee.accounting.services.AccountServiceLayer.AccountService
import io.circe.generic.auto._
import org.http4s._
import org.http4s.implicits.http4sKleisliResponseSyntaxOptionT
import org.http4s.server.Router
import org.http4s.server.blaze.BlazeServerBuilder
import sttp.tapir.generic.auto._
import sttp.tapir.json.circe._
import sttp.tapir.server.http4s.ztapir._
import sttp.tapir.swagger.http4s.SwaggerHttp4s
import sttp.tapir.ztapir._
import zio._
import zio.clock.Clock
import zio.interop.catz._

object AccountingApplication extends App {

  // Sample endpoint, with the logic implemented directly using .toRoutes
  val petEndpoint: ZEndpoint[Int, String, Account] =
    endpoint.get
      .in("pet" / path[Int]("petId"))
      .errorOut(stringBody)
      .out(jsonBody[Account])

  val petRoutes: HttpRoutes[RIO[AccountService with Clock, *]] =
    petEndpoint.toRoutes(petId => AccountService.find(petId))

  // Documentation
  val yaml: String = {
    import sttp.tapir.docs.openapi._
    import sttp.tapir.openapi.circe.yaml._
    List(petEndpoint).toOpenAPI("Our pets", "1.0").toYaml
  }

  // Starting the server
  val serve: ZIO[ZEnv with AccountService, Throwable, Unit] =
    ZIO.runtime[ZEnv with AccountService].flatMap { implicit runtime =>
      BlazeServerBuilder[RIO[AccountService with Clock, *]](
        runtime.platform.executor.asEC
      )
        .bindHttp(8080, "localhost")
        .withHttpApp(
          Router(
            "/" -> (petRoutes <+> new SwaggerHttp4s(yaml).routes)
          ).orNotFound
        )
        .serve
        .compile
        .drain
    }

  override def run(args: List[String]): URIO[zio.ZEnv, ExitCode] = {
    val repo = (zio.console.Console.live ++ DbSession.live) >>> AccountRepo.live
    serve.provideCustomLayer( (zio.console.Console.live ++ repo) >>> AccountService.live).exitCode
  }
}
