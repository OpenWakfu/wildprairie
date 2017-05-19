package com.github.wildprairie.common.actors.world

import akka.actor.{Actor, ActorLogging, ActorRef, Props}
import com.github.wakfutcp.traits.StatefulActor

/**
  * Created by hussein on 18/05/17.
  */
object WorldHandler {
  def props(client: ActorRef, server: ActorRef, authenticator: ActorRef): Props =
    Props(classOf[WorldHandler], client, server, authenticator)

  sealed trait WorldState
  final case object ProtocolVerification extends WorldState
}

class WorldHandler(client: ActorRef, server: ActorRef, authenticator: ActorRef)
  extends Actor with StatefulActor with ActorLogging {
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
