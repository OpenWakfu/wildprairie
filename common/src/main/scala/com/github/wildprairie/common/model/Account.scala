package com.github.wildprairie.common.model

import com.github.wakfutcp.protocol.common.Community

final case class Account(
  id: Int,
  login: String,
  hashedPassword: String,
  community: Community
)

object Account {
  import com.github.wildprairie.common.model.Database.context._

  implicit val encodeCommunity: MappedEncoding[Community, Int] =
    MappedEncoding(_.value)
  implicit val decodeCommunity: MappedEncoding[Int, Community] =
    MappedEncoding(Community.withValue)

  def getAccountByLogin(login: String): Quoted[Query[Account]] =
    quote {
      query[Account]
        .filter(_.login == lift(login))
    }

  def getLastCreated: Quoted[Query[Account]] =
    quote {
      query[Account]
        .sortBy(_.id)(Ord.desc)
    }

  def createAccount(account: Account): Quoted[Action[Account]] =
    quote {
      query[Account]
        .insert(account)
    }
}
