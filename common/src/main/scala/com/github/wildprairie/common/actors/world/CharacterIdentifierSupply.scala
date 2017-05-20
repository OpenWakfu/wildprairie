package com.github.wildprairie.common.actors.world

import akka.actor.Props

/**
  * Created by jacek on 20.05.17.
  */
object CharacterIdentifierSupply {
  def props: Props = Props(classOf[CharacterIdentifierSupply])

  final case class State(idCounter: Long, reservedNames: List[String])

  sealed trait Cmd

  final case class ReserveCharacter(name: String) extends Cmd

  sealed trait Evt

  final case class ReservedCharacter(name: String) extends Evt

  sealed trait CreationResult

  final case class Success(id: Long) extends CreationResult

  case object NameIsTaken extends CreationResult

  case object NameIsInvalid extends CreationResult
}

class CharacterIdentifierSupply extends SemiPersistentActor {
  import CharacterIdentifierSupply._

  override type State = CharacterIdentifierSupply.State
  override type Event = CharacterIdentifierSupply.Evt

  override def initialState: State =
    State(0, List())

  override def updateState(
    st: State,
    ev: Event
  ): State = ev match {
    case ReservedCharacter(name) =>
      st.copy(
        idCounter = st.idCounter + 1,
        reservedNames = name :: st.reservedNames
      )
  }

  override def elseReceiveCommand: Receive = {
    case ReserveCharacter(name) =>
      if(getState.reservedNames.contains(name)) {
        sender() ! NameIsTaken
      } else {
        persist(ReservedCharacter(name))
        sender() ! Success(getState.idCounter)
      }
  }

  override def persistenceId: String = "character-id-supply"
}
