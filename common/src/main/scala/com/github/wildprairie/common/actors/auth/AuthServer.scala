package com.github.wildprairie.common.actors.auth

import akka.actor.{ActorRef, Props}
import com.github.wildprairie.common.actors.shared.{WakfuServer, WorldServerSpec}

/**
  * Created by hussein on 16/05/17.
  */
object AuthServer {
  def props(authenticator: ActorRef): Props =
    Props(classOf[AuthServer], authenticator)

  sealed trait Message
  final case class UpdateWorldStatus(spec: WorldServerSpec) extends Message
  final case object GetWorldsSpec extends Message
  final case class WorldsSpec(worldsSpec: List[WorldServerSpec]) extends Message
}

final class AuthServer(authenticator: ActorRef) extends WakfuServer {

  import com.github.wildprairie.common.actors.auth.AuthServer._

  override def preStart(): Unit = {
    super.preStart()
    context.become(handleClusterEvents(List.empty))
  }

  override def newHandlerProps: (ActorRef) => Props =
    client => AuthHandler.props(client, self, authenticator)

  override def host: String =
    context.system.settings.config.getString("auth.host")

  override def port: Int =
    context.system.settings.config.getInt("auth.port")

  def handleClusterEvents(worldsSpec: List[WorldServerSpec]): Receive = {
    case UpdateWorldStatus(spec) =>
      log.info(s"world status update: $spec")
      context.become(
        handleClusterEvents(spec :: worldsSpec.filter(_.info.serverId != spec.info.serverId)))

    case GetWorldsSpec =>
      sender ! WorldsSpec(worldsSpec)

    case msg @ _ =>
      log.info(s"cluster event: $msg")
  }
}
