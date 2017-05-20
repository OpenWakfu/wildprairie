package com.github.wildprairie.common.actors.world

import akka.actor.Props
import com.github.wakfutcp.protocol.raw.CharacterDataSet.ForCharacterListSet

/**
  * Created by jacek on 20.05.17.
  */
// example usage of the semi-persistent actor concept
object AccountHandler {

  def props(id: Int): Props =
    Props(classOf[AccountHandler], id)

  case class State(characters: List[ForCharacterListSet])

  sealed trait Cmd

  case class CreateCharacter(set: ForCharacterListSet) extends Cmd

  case class DeleteCharacter(id: Long) extends Cmd

  case object GetCharacters extends Cmd

  case class CharacterList(chars: List[ForCharacterListSet])

  sealed trait Evt

  case class CharacterCreated(set: ForCharacterListSet) extends Evt
}

// account handler gets initialized with the account id
// persistence id is unique and derived from the id
// upon instantiation it replays it's history (most likely loading the great part of it from a snapshot)
class AccountHandler(val accountId: Int) extends SemiPersistentActor {
  import AccountHandler._
  override type State = AccountHandler.State
  override type Event = AccountHandler.Evt

  override def initialState: AccountHandler.State =
    AccountHandler.State(List())

  // handle commands, turn them into events and call persist on them
  override def elseReceiveCommand: Receive = {
    case CreateCharacter(set) =>
      persist(CharacterCreated(set))
    case GetCharacters =>
      sender() ! CharacterList(getState.characters)
  }

  // define how events affect the state (this is called after events are persisted)
  override def updateState(
    state: State,
    ev: Event
  ): State = ev match {
    case CharacterCreated(set) =>
      state.copy(characters = set :: state.characters)
  }

  override val persistenceId = s"account-$accountId"
}
