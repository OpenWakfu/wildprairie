package com.github.wildprairie.common.actors.world

import akka.actor.{ActorRef, Props, Stash}
import com.github.wakfutcp.protocol.raw.CharacterDataSet.ForCharacterListSet
import com.github.wildprairie.common.actors.world.Character.{CharacterCreationData, GetCharacterListData}

import scala.collection.mutable.ListBuffer

/**
  * Created by jacek on 20.05.17.
  */
object Account {

  def props(id: Int): Props =
    Props(classOf[Account], id)

  final case class State(characters: List[Long])

  sealed trait Cmd

  final case class NewCharacter(data: CharacterCreationData) extends Cmd

  final case class DeleteCharacter(id: Long) extends Cmd

  case object GetCharacters extends Cmd

  final case class CharacterList(chars: List[ForCharacterListSet])

  sealed trait Evt

  final case class CharacterCreated(data: CharacterCreationData) extends Evt
}

class Account(accountId: Int) extends SemiPersistentActor with Stash {
  import Account._

  override type State = Account.State
  override type Event = Account.Evt

  override def initialState: State = State(List())

  val characterRefs: ListBuffer[ActorRef] = ListBuffer()

  override def recoveryCompleted(): Unit = {
    if(characterRefs.isEmpty && getState.characters.nonEmpty) {
      // if we're loading from a snapshot
      for(cid <- getState.characters)
        characterRefs += context.actorOf(Character.props(cid, accountId))
    }
  }

  override def elseReceiveCommand: Receive = {
    case NewCharacter(data) =>
      persist(CharacterCreated(data))
    case GetCharacters =>
      if(characterRefs.nonEmpty) {
        characterRefs.foreach(_ ! GetCharacterListData)
        context.become(dispatchCharacterList(sender())(Nil, characterRefs.length))
      } else {
        sender() ! CharacterList(Nil)
      }
  }

  def dispatchCharacterList(asker: ActorRef)(list: List[ForCharacterListSet], remaining: Int): Receive = {
    case set: ForCharacterListSet =>
      if(remaining > 1) {
        context.become(dispatchCharacterList(asker)(set :: list, remaining - 1))
      }
      else {
        unstashAll()
        asker ! CharacterList(set :: list)
        context.become(elseReceiveCommand)
      }
    case _ => stash()
  }

  // define how events affect the state (this is called after events are persisted)
  override def updateState(
    state: State,
    ev: Event
  ): State = ev match {
    case CharacterCreated(data) =>
      val ref = context.actorOf(Character.props(data, accountId))

      characterRefs += ref
      state.copy(characters = data.id :: state.characters)
  }

  override val persistenceId = s"account-$accountId"
}
