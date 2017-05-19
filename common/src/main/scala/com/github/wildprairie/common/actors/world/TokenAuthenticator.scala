package com.github.wildprairie.common.actors.world

import java.util.UUID

import akka.actor.{Actor, ActorLogging, Props}
import com.github.wakfutcp.protocol.messages.forClient.AccountInformation
import com.github.wakfutcp.protocol.messages.forServer.ClientAuthenticationTokenMessage
import com.github.wildprairie.common.actors.shared.Authenticator

/**
  * Created by hussein on 19/05/17.
  */
object TokenAuthenticator {
  def props(): Props =
    Props(classOf[TokenAuthenticator])

  sealed trait Message
  final case class TokenGenerationRequest(account: AccountInformation) extends Message
  final case class TokenGenerationResult(token: String, account: AccountInformation) extends Message
  final case class GetAccountRequest(token: String) extends Message
  final case class GetAccountResult(token: String, accountOpt: Option[AccountInformation]) extends Message
}

class TokenAuthenticator extends Actor
  with ActorLogging with Authenticator[ClientAuthenticationTokenMessage, AccountInformation] {
  import TokenAuthenticator._

  override def receive: Receive = PartialFunction.empty

  override def preStart(): Unit = {
    context.become(handleRequest(Map()))
  }

  def handleRequest(tokens: Map[String, AccountInformation]): Receive = {
    case TokenGenerationRequest(account) =>
      val token = UUID.randomUUID().toString()
      sender ! TokenGenerationResult(token, account)
      context.become(handleRequest(tokens + (token -> account)))

    case GetAccountRequest(token) =>
      tokens.get(token) match {
        case Some(account) =>
          sender ! GetAccountResult(token, Some(account))
          context.become(handleRequest(tokens - token))

        case None =>
          sender ! GetAccountResult(token, None)
      }
  }
}