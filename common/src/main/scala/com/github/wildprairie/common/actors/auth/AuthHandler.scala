package com.github.wildprairie.common.actors.auth

import java.nio.ByteBuffer
import java.security.PrivateKey

import akka.actor.{Actor, ActorLogging, ActorRef, Props, Stash}
import com.github.wakfutcp.protocol.Decoder
import com.github.wakfutcp.traits.server.syntax._
import com.github.wakfutcp.protocol.messages.forClient._
import com.github.wakfutcp.protocol.messages.forServer.ClientDispatchAuthenticationMessage.CredentialData
import com.github.wakfutcp.protocol.messages.forServer._
import com.github.wakfutcp.traits.StatefulActor
import com.github.wildprairie.common.actors.auth.AccountAuthenticator.UserAccount
import com.github.wildprairie.common.actors.shared.{Authenticator, WorldServerSpec}
import com.github.wildprairie.common.actors.world.TokenAuthenticator
import com.github.wildprairie.common.traits.SaltGenerator
import com.github.wildprairie.common.utils._

/**
  * Created by hussein on 16/05/17.
  */
object AuthHandler {
  def props(client: ActorRef, server: ActorRef, authenticator: ActorRef): Props =
    Props(classOf[AuthHandler], client, server, authenticator)

  sealed trait AuthState
  final case object ProtocolVerification extends AuthState
  final case object AwaitingPublicKey extends AuthState
  final case class AwaitingLogin(salt: Long, privateKey: PrivateKey) extends AuthState
  final case class CheckingAuthentication(data: ClientDispatchAuthenticationMessage.CredentialData)
      extends AuthState
  final case class AcquiringWorldsInfo(account: UserAccount) extends AuthState
  final case class SelectingWorld(worldsSpec: List[WorldServerSpec], account: UserAccount)
      extends AuthState
  final case class AwaitingWorldAck(spec: WorldServerSpec) extends AuthState
}

class AuthHandler(client: ActorRef, server: ActorRef, authenticator: ActorRef)
    extends Actor
    with StatefulActor
    with ActorLogging
    with Stash
    with SaltGenerator {
  import AuthHandler._

  override type State = AuthState

  override def stateToReceive(state: AuthState): StatefulReceive =
    state match {
      case ProtocolVerification =>
        handleProtocol
      case AwaitingPublicKey =>
        handlePublicKeyRequest
      case AwaitingLogin(salt, privateKey) =>
        handleAuthentication(salt, privateKey)
      case CheckingAuthentication(data) =>
        handleAuthenticationCheck(data)
      case AcquiringWorldsInfo(account) =>
        handleWorldsSpecAcquisition(account)
      case SelectingWorld(worldsSpec, account) =>
        handleWorldSelection(worldsSpec, account)
      case AwaitingWorldAck(spec) =>
        handleWorldAck(spec)
    }

  override def initialState: List[AuthState] = List(ProtocolVerification)

  def handleProtocol: StatefulReceive =
    _ => {
      case ClientVersionMessage(versionWithBuild) =>
        log.info(s"protocol: version=$versionWithBuild")
        client !! ClientVersionResultMessage(success = true, versionWithBuild.version)
        setStates(List(AwaitingPublicKey))
    }

  def handlePublicKeyRequest: StatefulReceive =
    _ => {
      case ClientPublicKeyRequestMessage(serverId) =>
        log.info(s"public key req: serverId=$serverId")
        val salt = nextSalt
        client !! ClientPublicKeyMessage(salt, Cipher.RSA.generatePublic.getEncoded)
        setStates(List(AwaitingLogin(salt, Cipher.RSA.generatePrivate)))
    }

  def handleAuthentication(salt: Long, privateKey: PrivateKey): StatefulReceive =
    _ => {
      case ClientDispatchAuthenticationMessage(encryptedCredentials) =>
        val decryptedCredentials = Cipher.RSA.decrypt(privateKey, encryptedCredentials)
        val data = Decoder[CredentialData].decode(ByteBuffer.wrap(decryptedCredentials))
        log.info(s"auth: data=$data")
        authenticator ! Authenticator.Authenticate(data)
        setStates(List(CheckingAuthentication(data)))
    }

  def handleAuthenticationCheck(
    data: ClientDispatchAuthenticationMessage.CredentialData): StatefulReceive = {
    import ClientDispatchAuthenticationResultMessage._
    _ =>
      {
        case Authenticator.Success(_, account: UserAccount) =>
          log.info(s"auth: login successfully, account=$account")
          client !! ClientDispatchAuthenticationResultMessage(
            Result.Success,
            Some(account.info)
          )
          unstashAll()
          server ! AuthServer.GetWorldsSpec
          setStates(List(AcquiringWorldsInfo(account)))

        case Authenticator.Failure(_, reason) =>
          log.info(s"auth: failure, reason=$reason")
          val result = reason match {
            case Authenticator.FailureReason.WrongCredentials =>
              Result.InvalidLogin
            case Authenticator.FailureReason.AlreadyConnected =>
              Result.Unknown
            case Authenticator.FailureReason.Banned =>
              Result.Banned
            case Authenticator.FailureReason.UnknownException =>
              Result.InvalidLogin
          }
          client !! ClientDispatchAuthenticationResultMessage(result, None)

        case _ =>
          stash()
      }
  }

  def handleWorldsSpecAcquisition(account: UserAccount): StatefulReceive = { _ =>
    {
      case AuthServer.WorldsSpec(worldsSpec) =>
        log.info(s"worlds spec: $worldsSpec")
        unstashAll()
        setStates(List(SelectingWorld(worldsSpec, account)))

      case _ =>
        stash()
    }
  }

  def handleWorldSelection(worldsSpec: List[WorldServerSpec],
                           account: UserAccount): StatefulReceive =
    _ => {
      case ClientProxiesRequestMessage() =>
        log.info("proxies req")
        client !! ClientProxiesResultMessage(
          worldsSpec.map(_.proxy).toArray,
          worldsSpec.map(_.info).toArray
        )

      case AuthenticationTokenRequestMessage(serverId, accountId) =>
        log.info(s"selected world: id=$serverId, accountId=$accountId")
        worldsSpec.find(_.info.serverId == serverId) match {
          case Some(spec) =>
            // TODO: timeout on token generation ack
            spec.tokenAuthenticatorActor ! TokenAuthenticator.TokenGenerationRequest(account)
            setStates(List(AwaitingWorldAck(spec)))

          case None =>
            log.warning(s"server not found: id=$serverId")
            client !! AuthenticationTokenResultMessage.Failure
        }
    }

  def handleWorldAck(worldSpec: WorldServerSpec): StatefulReceive =
    _ => {
      case TokenAuthenticator.TokenGenerationResult(token, _) =>
        log.info(s"token ack received, dispatching client")
        client !! AuthenticationTokenResultMessage.Success(token)
    }
}
