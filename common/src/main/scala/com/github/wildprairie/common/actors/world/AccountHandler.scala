package com.github.wildprairie.common.actors.world

/**
  * Created by jacek on 20.05.17.
  */

// example usage of the semi-persistent actor concept
object AccountHandler {
  sealed trait State
  sealed trait Event
}

// account handler gets initialized with the account id
// persistence id is unique and derived from the id
// upon instantiation it replays it's history (most likely loading the great part of it from a snapshot)
class AccountHandler(val accountId: Int) extends SemiPersistentActor {
  override type State = AccountHandler.State
  override type Event = AccountHandler.Event

  // handle commands, turn them into events and call persist on them
  override def elseReceiveCommand: Receive = ???

  // define how events affect the state (this is called after events are persisted)
  override def updateState(
    state: State,
    ev: Event
  ): State = ???

  override val persistenceId = s"account-$accountId"
}
