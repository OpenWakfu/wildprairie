package com.github.wildprairie.common.actors.world

import akka.actor.{ActorRef, Props, Stash}
import com.github.wakfutcp.protocol.raw.CharacterDataSet.ForCharacterListSet
import com.github.wildprairie.common.actors.shared.ActorFolder
import com.github.wildprairie.common.actors.world.Character.{
  CharacterCreationData,
  GetCharacterListData
}

import scala.collection.mutable.ListBuffer

/**
  * Created by jacek on 20.05.17.
  */
object User {

  def props(id: Int): Props =
    Props(classOf[User], id)

  final case class State(characters: List[Long])

  sealed trait Cmd

  final case class NewCharacter(data: CharacterCreationData) extends Cmd

  final case class DeleteCharacter(id: Long) extends Cmd

  case object GetCharacters extends Cmd

  final case class CharacterList(chars: List[ForCharacterListSet])

  sealed trait Evt

  final case class CharacterCreated(data: CharacterCreationData) extends Evt
}

class User(accountId: Int) extends SemiPersistentActor with ActorFolder with Stash {
  import User._

  override type State = User.State
  override type Event = User.Evt

  override def initialState: State = State(Nil)

  override def persistenceId: String = s"account-$accountId"

  val characterRefs: ListBuffer[ActorRef] = ListBuffer()

  override def recoveryCompleted(): Unit = {
    if (characterRefs.isEmpty && getState.characters.nonEmpty) {
      // we only create actors when we've finished recovering
      // and when a new character is created, never in event processing
      for (cid <- getState.characters)
        characterRefs += context.actorOf(Character.props(cid, accountId))
    }
  }

  override def elseReceiveCommand: Receive = {
    case NewCharacter(data) =>
      val ref = context.actorOf(Character.props(data, accountId))
      characterRefs += ref

      persist(CharacterCreated(data))

    case GetCharacters =>
      import akka.pattern._
      import context.dispatcher

      fold[List[ForCharacterListSet]](characterRefs, GetCharacterListData)(Nil) {
        case (set: ForCharacterListSet, acc) =>
          set :: acc
      }.map(CharacterList)
        .pipeTo(sender())
  }

  // define how events affect the state (this is called after events are persisted)
  override def updateState(
    state: State,
    ev: Event
  ): State = ev match {
    case CharacterCreated(data) =>
      state.copy(characters = data.id :: state.characters)
  }
}
