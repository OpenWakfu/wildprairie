package com.github.wildprairie.common.actors.world

import akka.actor.{ActorRef, Props}
import akka.cluster.ClusterEvent.MemberUp
import com.github.wakfutcp.protocol.common.{Community, Proxy, ProxyServer, Version, WorldInfo}
import com.github.wildprairie.common.actors.ActorPaths
import com.github.wildprairie.common.actors.auth.AuthServer
import com.github.wildprairie.common.actors.shared.{WakfuServer, WorldServerSpec}

/**
  * Created by hussein on 18/05/17.
  */
object WorldServer {
  def props(authenticator: ActorRef): Props =
    Props(classOf[WorldServer], authenticator)

  sealed trait Message
}

class WorldServer(authenticator: ActorRef) extends WakfuServer {
  import WakfuServer._

  // startup the global character identifier supply
  context.actorOf(CharacterIdentifierSupply.props, ActorPaths.World.CharacterIdentitySupplier.name)

  override def receive: Receive =
    handleClusterEvents

  override def newHandlerProps: (ActorRef) => Props =
    client => WorldHandler.props(client, self, authenticator)

  override def host: String =
    spec.proxy.server.address

  override def port: Int =
    spec.proxy.server.ports(0)

  lazy val spec: WorldServerSpec = {
    val config = context.system.settings.config
    val id = config.getInt("world.id")
    val name = config.getString("world.name")
    val community = Community.withValue(config.getInt("world.community"))
    val locked = config.getBoolean("world.locked")
    val version = config.getString("world.version").split("\\.")
    new WorldServerSpec(
      cluster.selfAddress,
      WorldInfo(
        id,
        Version.WithBuild(
          Version(
            version(0).toByte,
            version(1).toShort,
            version(2).toByte
          ),
          "-1"
        ),
        Array.empty,
        locked
      ),
      Proxy(
        id,
        name,
        community,
        ProxyServer(config.getString("world.host"), Array(config.getInt("world.port"))),
        0
      )
    )
  }

  def handleClusterEvents: Receive = {
    case MemberUp(member) if member.hasRole(ROLE_AUTH) =>
      log.info(s"dispatching world status to $member")
      val authServer =
        context.actorSelection(member.address + ActorPaths.Auth.AuthServer.toString())
      authServer ! AuthServer.UpdateWorldStatus(spec)

    case msg @ _ =>
      log.info(s"cluster event: $msg")
  }
}
