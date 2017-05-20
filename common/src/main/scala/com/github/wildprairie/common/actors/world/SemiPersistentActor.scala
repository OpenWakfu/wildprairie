package com.github.wildprairie.common.actors.world

import akka.persistence._

/**
  * Created by jacek on 20.05.17.
  */
// this is just a concept of implementation
trait SemiPersistentActor extends PersistentActor {
  type State

  type Event

  private[this] var state: State = initialState

  val snapShotInterval = 10000

  def recoveryCompleted(): Unit = ()

  def initialState: State

  def updateState(st: State, ev: Event): State

  def getState: State = state

  def elseReceiveCommand: Receive

  override def receiveRecover: Receive = {
    case SnapshotOffer(meta, snapshot) =>
      state = snapshot.asInstanceOf[State]
    case RecoveryCompleted => recoveryCompleted()
    case ev => state = updateState(state, ev.asInstanceOf[Event])
  }

  final def persist(e: Event): Unit =
    persistAsync(e) { e =>
      state = updateState(state, e)
      if (lastSequenceNr % snapShotInterval == 0 && lastSequenceNr != 0)
        saveSnapshot(state)
    }

  private[this] def handleSnapshotCommands: Receive = {
    case SaveSnapshotSuccess(metadata) =>
      // delete all messages up to the point when the snapshot happened
      deleteMessages(metadata.sequenceNr)
    // TODO: we can probably delete old unneeded snapshots too here
    case SaveSnapshotFailure(metadata, reason) =>
    //
  }

  override def receiveCommand: Receive =
    handleSnapshotCommands orElse elseReceiveCommand
}
