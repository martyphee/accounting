package com.accounting.domain

import eu.timepit.refined.types.string.NonEmptyString
import io.estatico.newtype.macros.newtype

import java.time.LocalDateTime
import java.util.UUID
import scala.util.control.NoStackTrace

object account {
  @newtype case class AccountId(value: UUID)
  @newtype case class AccountName(value: String)
  @newtype case class CreatedAt(value: LocalDateTime)
  @newtype case class UpdatedAt(value: LocalDateTime)

  case class Account(
    uuid: AccountId,
    name: AccountName,
    createdAt: CreatedAt,
    updatedAt: UpdatedAt
  )

  case class AccountCreate(
    uuid: AccountId,
    name: AccountName
  )

  case class InvalidAccount(value: String) extends NoStackTrace

  @newtype case class AccountIdParm(value: NonEmptyString) {
    def toDomain: AccountId = AccountId(UUID.fromString(value.value))
  }
}
