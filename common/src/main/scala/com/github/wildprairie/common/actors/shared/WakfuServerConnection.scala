package com.github.wildprairie.common.actors.shared

import akka.actor.{ActorRef, Props}
import com.github.wakfutcp.actors.server.ConnectionHandler
import com.github.wakfutcp.traits.server.WakfuServerMessageReceiving

/**
  * Created by hussein on 16/05/17.
  */
object WakfuServerConnection {
  def props(connection: ActorRef, handler: ActorRef): Props =
    Props(classOf[WakfuServerConnection], connection, handler)
}

final class WakfuServerConnection(connection: ActorRef, handler: ActorRef)
    extends ConnectionHandler(connection, handler)
    with WakfuServerMessageReceiving {}
