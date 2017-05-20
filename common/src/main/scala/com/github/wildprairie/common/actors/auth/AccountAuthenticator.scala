package com.github.wildprairie.common.actors.auth

import akka.actor.{Actor, ActorLogging, Props}
import com.github.wakfutcp.protocol.messages.forClient.AccountInformation
import com.github.wakfutcp.protocol.messages.forServer.ClientDispatchAuthenticationMessage
import com.github.wakfutcp.protocol.protobuf.account.Status
import com.github.wildprairie.common.actors.auth.AccountAuthenticator.UserAccount
import com.github.wildprairie.common.actors.shared.Authenticator
import com.github.wildprairie.common.model.Account
import io.github.nremond.SecureHash

import scala.concurrent.Future

/**
  * Created by hussein on 19/05/17.
  */
object AccountAuthenticator {
  def props(): Props =
    Props(classOf[AccountAuthenticator])

  final case class UserAccount(account: Account, info: AccountInformation)
}

class AccountAuthenticator
    extends Actor
    with ActorLogging
    with Authenticator[ClientDispatchAuthenticationMessage.CredentialData, UserAccount] {
  import Authenticator._
  import context.dispatcher

  def validateAccount(login: String, password: String): Future[Option[Account]] = {
    import com.github.wildprairie.common.model.Database.context._
    import Account._

    run(Account.getAccountByLogin(login)).map { opt =>
      opt.headOption.flatMap { acc =>
        if (SecureHash.validatePassword(password, acc.hashedPassword))
          Some(acc)
        else None
      }
    }
  }

  override def receive: Receive = {
    case Authenticate(user: ClientDispatchAuthenticationMessage.CredentialData) =>
      import akka.pattern._

      validateAccount(user.login, user.password).map {
        case Some(acc) =>
          Success(
            user,
            UserAccount(
              acc,
              AccountInformation(acc.community, None, Status(Map()))
            )
          )
        case None =>
          Failure(user, FailureReason.WrongCredentials)
      }.pipeTo(sender())
  }
}
