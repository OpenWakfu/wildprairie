package com.github.wildprairie.common.actors.auth

import java.nio.ByteBuffer
import java.security.PrivateKey

import akka.actor.{ActorRef, Props, Stash}
import com.github.wakfutcp.protocol.Decoder
import com.github.wakfutcp.traits.server.syntax._
import com.github.wakfutcp.protocol.messages.forClient._
import com.github.wakfutcp.protocol.messages.forServer.ClientDispatchAuthenticationMessage.CredentialData
import com.github.wakfutcp.protocol.messages.forServer._
import com.github.wildprairie.common.actors.common.{Authenticator, WakfuHandler, WorldServerSpec}
import com.github.wildprairie.common.utils._

import scala.util.Random


/**
  * Created by hussein on 16/05/17.
  */
object AuthHandler {
  def props(authenticator: ActorRef): Props =
    Props(classOf[AuthHandler], authenticator)

  sealed trait AuthState
  final case object ProtocolVerification extends AuthState
  final case object AwaitingPublicKey extends AuthState
  final case class AwaitingLogin(salt: Long, privateKey: PrivateKey) extends AuthState
  final case class CheckingAuthentication(client: ActorRef, data: ClientDispatchAuthenticationMessage.CredentialData) extends AuthState
  final case class AcquiringWorldsInfo(account: AccountInformation) extends AuthState
  final case class SelectingWorld(worldsSpec: List[WorldServerSpec], account: AccountInformation) extends AuthState

  private val rand = new Random()

  def nextSalt: Long = rand.nextLong()
}

class AuthHandler(authenticator: ActorRef) extends WakfuHandler(authenticator) with Stash {
  import AuthHandler._

  override type State = AuthState

  override def stateToReceive(state: AuthState): StatefulReceive =
    state match {
      case ProtocolVerification =>
        handleProtocol
      case AwaitingPublicKey =>
        handlePublicKeyReq
      case AwaitingLogin(salt, privateKey) =>
        handleAuthentication(salt, privateKey)
      case CheckingAuthentication(client, data) =>
        handleAuthenticationCheck(client, data)
      case AcquiringWorldsInfo(account) =>
        handleWorldsSpecAcquisition(account)
      case SelectingWorld(worldsSpec, account) =>
        handleWorldSelection(worldsSpec, account)
    }

  override def initialState: List[AuthState] = List(ProtocolVerification)

  def handleProtocol : StatefulReceive =
    _ => {
      case ClientVersionMessage(versionWithBuild) =>
        log.info(s"protocol: version=$versionWithBuild")
        sender !! ClientVersionResultMessage(success = true, versionWithBuild.version)
        setStates(List(AwaitingPublicKey))
    }

  def handlePublicKeyReq: StatefulReceive =
    _ => {
      case ClientPublicKeyRequestMessage(serverId) =>
        log.info(s"public key req: serverId=$serverId")
        val salt = nextSalt
        sender !! ClientPublicKeyMessage(salt, Cipher.RSA.generatePublic.getEncoded)
        setStates(List(AwaitingLogin(salt, Cipher.RSA.generatePrivate)))
    }

  def handleAuthentication(salt: Long, privateKey: PrivateKey): StatefulReceive =
    _ => {
      case ClientDispatchAuthenticationMessage(encryptedCredentials) =>
        val decryptedCredentials = Cipher.RSA.decrypt(privateKey, encryptedCredentials)
        val data = Decoder[CredentialData].decode(ByteBuffer.wrap(decryptedCredentials))
        log.info(s"auth: data=$data")
        authenticator ! Authenticator.Authenticate(data)
        setStates(List(CheckingAuthentication(sender, data)))
    }

  def handleAuthenticationCheck(client: ActorRef, data: ClientDispatchAuthenticationMessage.CredentialData): StatefulReceive = {
    import ClientDispatchAuthenticationResultMessage._
    _ => {
      case Authenticator.Success(_, account: AccountInformation) =>
        log.info(s"auth: login successfully, account=$account")
        client !! ClientDispatchAuthenticationResultMessage(
          Result.Success,
          Some(account)
        )
        unstashAll()
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

  def handleWorldsSpecAcquisition(account: AccountInformation): StatefulReceive = {
    _ => {
      case AuthServer.WorldsSpec(worldsSpec) =>
        unstashAll()
        setStates(List(SelectingWorld(worldsSpec, account)))

      case _ =>
        stash()
    }
  }

  def handleWorldSelection(worldsSpec: List[WorldServerSpec], account: AccountInformation): StatefulReceive =
    _ => {
      case ClientProxiesRequestMessage() =>
        log.info("proxies req")
        sender !! ClientProxiesResultMessage(
          worldsSpec.map(_.proxy).toArray,
          worldsSpec.map(_.info).toArray
        )

      case AuthenticationTokenRequestMessage(serverId, accountId) =>
        log.info(s"selected world: id=$serverId, accountId=$accountId")
    }
}
