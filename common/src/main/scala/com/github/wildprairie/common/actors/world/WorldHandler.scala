package com.github.wildprairie.common.actors.world

import java.security.PrivateKey

import akka.actor.{ActorLogging, ActorRef, Props, Stash}
import akka.io.Tcp
import com.github.wakfutcp.protocol.messages.forClient._
import com.github.wakfutcp.protocol.messages.forServer._
import com.github.wakfutcp.protocol.raw.CharacterDataSet.ForCharacterListSet
import com.github.wakfutcp.traits.StatefulActor
import com.github.wakfutcp.traits.server.syntax._
import com.github.wildprairie.common.actors.auth.AccountAuthenticator.UserAccount
import com.github.wildprairie.common.actors.shared.Authenticator
import com.github.wildprairie.common.actors.shared.Authenticator.{Failure, FailureReason, Success}
import com.github.wildprairie.common.actors.world.Account._
import com.github.wildprairie.common.actors.world.Character.CharacterCreationData
import com.github.wildprairie.common.actors.world.CharacterIdentifierSupply.ReserveCharacter
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
  final case class CharacterSelection(account: ActorRef, owner: UserAccount) extends WorldState
}

class WorldHandler(client: ActorRef, server: ActorRef, authenticator: ActorRef)
    extends StatefulActor
    with ActorLogging
    with Stash
    with SaltGenerator {
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
      case CharacterSelection(handler, account) =>
        handleCharacterSelection(handler, account)
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
      // client sends GiftInventoryRequestMessage
      case Success(_, account: UserAccount) =>
        import akka.pattern._
        import context.dispatcher
        import scala.concurrent.duration._

        // TODO: account information serialization
        // TODO: retrieve account characters
        log.info(s"authentication success: account=$account")
        client !! ClientAuthenticationResultsMessage.Success(Array[Byte](0))
        client !! WorldSelectionResultMessage.Success
        // TODO: send: FreeCompanionBreedIdMessage, ClientCalendarSynchronizationMessage, ClientSystemConfigurationMessage
        // TODO: send: ClientAdditionalCharacterSlotsUpdateMessage, CompanionListMessage
        val handler = context.actorOf(Account.props(account.account.id))
        handler
          .?(GetCharacters)(5.seconds)
          .mapTo[CharacterList]
          .map { list =>
            Tcp.Write(CharactersListMessage(list.chars.toArray).wrap)
          }
          .pipeTo(client)
        unstashAll()
        setStates(List(CharacterSelection(handler, account)))

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

  def handleCharacterSelection(handler: ActorRef, account: UserAccount): StatefulReceive =
    _ => {
      case msg: CharacterCreationMessage =>
        import akka.pattern._
        import context.dispatcher
        import scala.concurrent.duration._

        val reservation =
          context
            .actorSelection("/user/world-server/character-id-supply")
            .?(ReserveCharacter(msg.name))(5.seconds)
            .mapTo[CharacterIdentifierSupply.ReservationResult]

        // respond the client
        reservation.map {
          case CharacterIdentifierSupply.Success(_) =>
            CharacterCreationResultMessage.Success
          case CharacterIdentifierSupply.NameIsTaken =>
            CharacterCreationResultMessage.NameIsTaken
          case CharacterIdentifierSupply.NameIsInvalid =>
            CharacterCreationResultMessage.NameIsInvalid
        }.map(m => Tcp.Write(m.wrap))
          .pipeTo(client)

        // let the account know
        reservation.collect {
          case CharacterIdentifierSupply.Success(id) =>
            NewCharacter(
              CharacterCreationData(
                id,
                msg.sex,
                msg.skinColorIndex,
                msg.hairColorIndex,
                msg.pupilColorIndex,
                msg.skinColorFactor,
                msg.hairColorFactor,
                msg.clothIndex,
                msg.faceIndex,
                msg.breed,
                msg.name
              )
            )
        }.pipeTo(handler)

      // on charac create server sends:
      // CharacterCreationResultMessage
      // EquipmentInventoryMessage
      // EquipmentAccountMessage
      // CharactersListMessage
      // CharacterSelectionResultMessage
      // HasModerationRequestMessage
      // FriendListMessage
      // IgnoreListMessage
      // ClientCharacterUpdateMessage
      // ClientNationSynchronizationMessage
      // GameAccountMessage
      // ClientNationSynchronizationMessage
      // BuildSheetNotificationMessage
      // 13252
      // AptitudeSheetNotificationMessage
      // CharacterInformationMessage
      // ItemInventoryResetMessage
      // ClientCharacterUpdateMessage
      // CharacterEnterWorldMessage
      // ItemIdCacheUpdateMessage
      // CharacterEnterPartitionMessage
      // ActorSpawnMessage
      // ielems
      // CharacterHealthUpdateMessage
      // CharacterUpdateMessage
      //
//      case CharacterDeletionMessage(id) =>
//        account ! DeleteCharacter(id)
      case msg @ _ =>
        log.info(s"received: $msg")
    }
}
