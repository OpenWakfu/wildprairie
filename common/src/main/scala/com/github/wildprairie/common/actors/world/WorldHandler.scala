package com.github.wildprairie.common.actors.world

import akka.actor.{ActorRef, Props}
import com.github.wildprairie.common.actors.common.WakfuHandler

/**
  * Created by hussein on 18/05/17.
  */
object WorldHandler {
  def props(authenticator: ActorRef): Props =
    Props(classOf[WorldHandler], authenticator)

  sealed trait WorldState

  final case object ProtocolVerification extends WorldState
}

class WorldHandler(authenticator: ActorRef)
  extends WakfuHandler(authenticator) {
  import WorldHandler._

  override type State = WorldState

  override def initialState: List[WorldState] = List(ProtocolVerification)

  override def stateToReceive(state: WorldState): StatefulReceive =
    state match {
      case ProtocolVerification =>
        handleProtocolVerification
    }

  def handleProtocolVerification: StatefulReceive =
    _ => {
      case msg@_ =>
        log.info(s"received $msg")
    }
}
