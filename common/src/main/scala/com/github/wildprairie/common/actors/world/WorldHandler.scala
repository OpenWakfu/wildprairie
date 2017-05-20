package com.github.wildprairie.common.actors.world

import java.security.PrivateKey

import akka.actor.{Actor, ActorLogging, ActorRef, Props, Stash}
import com.github.wakfutcp.protocol.messages.forClient._
import com.github.wakfutcp.protocol.messages.forServer.{ClientAuthenticationTokenMessage, ClientPublicKeyRequestMessage, ClientVersionMessage}
import com.github.wakfutcp.protocol.raw.CharacterDataSet.ForCharacterListSet
import com.github.wakfutcp.traits.StatefulActor
import com.github.wakfutcp.traits.server.syntax._
import com.github.wildprairie.common.actors.shared.Authenticator
import com.github.wildprairie.common.actors.shared.Authenticator.{Failure, FailureReason, Success}
import com.github.wildprairie.common.traits.SaltGenerator
import com.github.wildprairie.common.utils.Cipher

import scala.concurrent.duration.Duration

/**
  * Created by hussein on 18/05/17.
  */
object WorldHandler {
  def props(client: ActorRef, server: ActorRef, authenticator: ActorRef): Props =
    Props(classOf[WorldHandler], client, server, authenticator)

  sealed trait WorldState
  final case object ProtocolVerification extends WorldState
  final case object AwaitingPublicKey extends WorldState
  final case class AwaitingToken(salt: Long, privateKey: PrivateKey) extends WorldState
  final case class CheckingAuthentication(token: String) extends WorldState
  final case class SelectingCharacter(characters: Array[ForCharacterListSet]) extends WorldState
}

class WorldHandler(client: ActorRef, server: ActorRef, authenticator: ActorRef)
  extends Actor with StatefulActor with ActorLogging with Stash with SaltGenerator {
  import WorldHandler._

  override type State = WorldState

  override def initialState: List[WorldState] = List(ProtocolVerification)

  override def stateToReceive(state: WorldState): StatefulReceive =
    state match {
      case ProtocolVerification =>
        handleProtocolVerification
      case AwaitingPublicKey =>
        handlePublicKeyRequest
      case AwaitingToken(_, _) =>
        handleAuthentication
      case CheckingAuthentication(_) =>
        handleAuthenticationCheck
      case SelectingCharacter(characters) =>
        handleCharacterSelection(characters)
    }

  def handleProtocolVerification: StatefulReceive =
    _ => {
      case ClientVersionMessage(versionWithBuild) =>
        log.info(s"protocol verification: version=$versionWithBuild")
        client !! ClientVersionResultMessage(success = true, versionWithBuild.version)
        setStates(List(AwaitingPublicKey))
    }

  def handlePublicKeyRequest: StatefulReceive =
    _ => {
      case ClientPublicKeyRequestMessage(serverId) =>
        log.info(s"public key req: serverId=$serverId")
        val salt = nextSalt
        client !! ClientPublicKeyMessage(salt, Cipher.RSA.generatePublic.getEncoded)
        setStates(List(AwaitingToken(salt, Cipher.RSA.generatePrivate)))
    }

  def handleAuthentication: StatefulReceive =
    _ => {
      case ClientAuthenticationTokenMessage(token) =>
        log.info(s"checking authentication: token=$token")
        authenticator ! Authenticator.Authenticate(token)
        setStates(List(CheckingAuthentication(token)))
    }

  def handleAuthenticationCheck: StatefulReceive =
    _ => {
      case Success(_: String, account: AccountInformation) =>
        // TODO: account information serialization
        // TODO: retrieve account characters
        log.info(s"authentication success: account=$account")
        client !! ClientAuthenticationResultsMessage.Success(Array[Byte](0))
        client !! WorldSelectionResultMessage.Success
        client !! CharactersListMessage(Array.empty)
        unstashAll()
        setStates(List(SelectingCharacter(Array.empty)))

      case Failure(_: String, reason) =>
        log.warning(s"token authentication failure: reason=$reason")
        client !! {
          reason match {
            case FailureReason.AlreadyConnected =>
              ClientAuthenticationResultsMessage.AlreadyConnected
            case FailureReason.Banned =>
              ClientAuthenticationResultsMessage.AccountBanned(Duration.Zero)
            case _ =>
              ClientAuthenticationResultsMessage.InvalidToken
          }
        }

      case _ =>
        stash()
    }

  def handleCharacterSelection(characters: Array[ForCharacterListSet]): StatefulReceive =
    _ => {
      case msg@_ =>
        log.info(s"received: $msg")
    }
}
