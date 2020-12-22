package com.accounting.http.routes

import cats.Applicative
import com.accounting.domain.account._
import io.circe._
import io.circe.generic.semiauto._
import io.estatico.newtype.ops._
import io.estatico.newtype.Coercible
import org.http4s.EntityEncoder
import org.http4s.circe.jsonEncoderOf

object json extends JsonCodecs {
  implicit def deriveEntityEncoder[F[_]: Applicative, A: Encoder]: EntityEncoder[F, A] = jsonEncoderOf[F, A]
}

private[http] trait JsonCodecs {
  // ----- Coercible codecs -----
  implicit def coercibleDecoder[A: Coercible[B, *], B: Decoder]: Decoder[A] =
    Decoder[B].map(_.coerce[A])

  implicit def coercibleEncoder[A: Coercible[B, *], B: Encoder]: Encoder[A] =
    Encoder[B].contramap(_.repr.asInstanceOf[B])

  implicit def coercibleKeyDecoder[A: Coercible[B, *], B: KeyDecoder]: KeyDecoder[A] =
    KeyDecoder[B].map(_.coerce[A])

  implicit def coercibleKeyEncoder[A: Coercible[B, *], B: KeyEncoder]: KeyEncoder[A] =
    KeyEncoder[B].contramap[A](_.repr.asInstanceOf[B])


  implicit val accountDecoder: Decoder[Account] = deriveDecoder[Account]
  implicit val accountCreatedDecoder: Decoder[AccountCreateRequest] = deriveDecoder[AccountCreateRequest]
//  implicit val accountIdDecoder: Decoder[AccountId] = deriveDecoder[AccountId]
//  implicit val accountNameDecoder: Decoder[AccountName] = deriveDecoder[AccountName]

  implicit val accountEncoder: Encoder[Account] = deriveEncoder[Account]
//  implicit val accountIdEncoder: Encoder[AccountId] = deriveEncoder[AccountId]
//  implicit val accountNameEncoder: Encoder[AccountName] = deriveEncoder[AccountName]

}
