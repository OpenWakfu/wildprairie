package com.github.wildprairie.common.actors.world

import java.time.Instant
import java.util.UUID

import akka.actor.{Actor, ActorLogging, Props}
import com.github.wildprairie.common.actors.auth.AccountAuthenticator.UserAccount
import com.github.wildprairie.common.actors.shared.Authenticator

/**
  * Created by hussein on 19/05/17.
  */
object TokenAuthenticator {
  import scala.concurrent.duration._

  def props(): Props =
    Props(classOf[TokenAuthenticator])

  sealed trait Message
  final case class TokenGenerationRequest(account: UserAccount) extends Message
  final case class TokenGenerationResult(token: String, account: UserAccount) extends Message
  final case object CleanupTokens extends Message

  val TOKEN_CLEANUP_DELAY: FiniteDuration = 1.minute
  val TOKEN_TIMEOUT: FiniteDuration = 1.minute
}

class TokenAuthenticator extends Actor with ActorLogging with Authenticator[String, UserAccount] {
  import TokenAuthenticator._
  import Authenticator._
  import context._

  override def preStart(): Unit =
    system.scheduler.schedule(
      TOKEN_CLEANUP_DELAY,
      TOKEN_CLEANUP_DELAY,
      self,
      CleanupTokens
    )

  override def receive: Receive =
    handleRequest(Map())

  def handleRequest(tokens: Map[String, (Long, UserAccount)]): Receive = {
    case CleanupTokens =>
      log.info("cleaning up invalid tokens")
      val validTokens = tokens.filter(_._2._1 > Instant.now.getEpochSecond)
      context.become(handleRequest(validTokens))

    case TokenGenerationRequest(account) =>
      val token = UUID.randomUUID().toString
      val timeout = Instant.now.getEpochSecond + TOKEN_TIMEOUT.toSeconds
      val mapValue = (timeout, account)
      log.info(s"generated auth token: account=$account, token=$token")
      sender ! TokenGenerationResult(token, account)
      context.become(handleRequest(tokens + (token -> mapValue)))

    case Authenticate(token: String) =>
      log.info(s"authenticating user with token=$token")
      tokens.get(token) match {
        case Some((_, account)) =>
          sender ! Success(token, account)
          context.become(handleRequest(tokens - token))

        case None =>
          sender ! Failure(token, FailureReason.WrongCredentials)
      }
  }
}
