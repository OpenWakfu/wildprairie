package com.github.wildprairie.common.actors.shared

import java.net.InetSocketAddress

import akka.actor.{ActorRef, Props}
import com.github.wakfutcp.actors.server.TcpServer
import com.github.wildprairie.common.actors.ClusteredActor

/**
  * Created by hussein on 16/05/17.
  */
object WakfuServer {
  val ROLE_AUTH = "auth"
  val ROLE_WORLD = "world"
  val ROLE_MASTER = "master"
}

abstract class WakfuServer extends ClusteredActor {
  override def preStart(): Unit =
    super.preStart()
    context.actorOf(TcpServer.props(
      new InetSocketAddress(host, port),
      WakfuServerConnection.props,
      newHandlerProps
    ), "tcp-server")

  def newHandlerProps: (ActorRef) => Props
  def host: String
  def port: Int

  override def receive: Receive = PartialFunction.empty
}
