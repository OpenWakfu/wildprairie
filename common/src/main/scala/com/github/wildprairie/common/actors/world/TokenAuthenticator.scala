package com.github.wildprairie.common.actors.world

import java.util.UUID

import akka.actor.{Actor, ActorLogging, Props}
import com.github.wakfutcp.protocol.messages.forClient.AccountInformation
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
}

class TokenAuthenticator extends Actor
  with ActorLogging with Authenticator[String, AccountInformation] {
  import TokenAuthenticator._
  import Authenticator._

  override def receive: Receive =
    handleRequest(Map())

  def handleRequest(tokens: Map[String, AccountInformation]): Receive = {
    case TokenGenerationRequest(account) =>
      val token = UUID.randomUUID().toString()
      log.info(s"generated auth token: account=$account, token=$token")
      sender ! TokenGenerationResult(token, account)
      context.become(handleRequest(tokens + (token -> account)))

    case Authenticate(token: String) =>
      log.info(s"authenticating user with token=$token")
      tokens.get(token) match {
        case Some(account) =>
          sender ! Success(token, account)
          context.become(handleRequest(tokens - token))

        case None =>
          sender ! Failure(token, FailureReason.WrongCredentials)
      }
  }
}