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
  def props(host: String, port: Int, authenticator: ActorRef): Props =
    Props(classOf[WorldServer], host, port, authenticator)
}

class WorldServer(host: String, port: Int, authenticator: ActorRef)
  extends WakfuServer(host, port) {
  import WakfuServer._

  override def preStart(): Unit = {
    super.preStart()
    context.become(handleClusterEvents())
  }

  override def newHandlerProps: Props =
    WorldHandler.props(self, authenticator)

  def handleClusterEvents(): Receive = {
    case MemberUp(member) if member.hasRole(ROLE_AUTH) =>
      log.info(s"dispatching world status to $member")
      val authServer = context.actorSelection(member.address + ActorPaths.Auth.AuthServer.toString())
      authServer ! AuthServer.UpdateWorldStatus(
        new WorldServerSpec(
          new WorldInfo(0, Version.WithBuild(Version(1, 51, 1), "-1"), Array.empty, locked = false),
          new Proxy(0, "Kokokobana", Community.UK, ProxyServer(host, Array(port)), 0)
        )
      )

    case msg@_ =>
      log.info(s"cluster event: $msg")
  }
}
