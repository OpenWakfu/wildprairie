package com.github.wildprairie.common.actors.auth

import akka.actor.{Actor, ActorLogging, Props}
import com.github.wakfutcp.protocol.common.Community
import com.github.wakfutcp.protocol.messages.forClient.AccountInformation
import com.github.wakfutcp.protocol.messages.forServer.ClientDispatchAuthenticationMessage
import com.github.wakfutcp.protocol.protobuf.account.Status
import com.github.wildprairie.common.actors.shared.Authenticator

/**
  * Created by hussein on 19/05/17.
  */
object AccountAuthenticator {
  def props(): Props =
    Props(classOf[AccountAuthenticator])
}

class AccountAuthenticator extends Actor
    with ActorLogging with Authenticator[ClientDispatchAuthenticationMessage.CredentialData, AccountInformation]
{
  import Authenticator._

  override def receive: Receive = {
    case Authenticate(user: ClientDispatchAuthenticationMessage.CredentialData) =>
      // TODO: check in database
      sender ! Success(user, AccountInformation(Community.UK, None, Status(Map())))
  }
}
