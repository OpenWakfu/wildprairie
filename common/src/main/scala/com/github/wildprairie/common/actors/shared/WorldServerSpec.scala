package com.github.wildprairie.common.actors.shared

import akka.actor.{ActorContext, ActorSelection, Address}
import com.github.wakfutcp.protocol.common.{Proxy, WorldInfo}
import com.github.wildprairie.common.actors.ActorPaths

/**
  * Created by hussein on 18/05/17.
  */
final class WorldServerSpec(val baseAddress: Address, val info: WorldInfo, val proxy: Proxy) extends Serializable {
  def serverActor(implicit context: ActorContext): ActorSelection =
    select(ActorPaths.World.WorldServer)

  def tokenAuthenticatorActor(implicit context: ActorContext): ActorSelection =
    select(ActorPaths.World.Authenticator)

  def select(actorMeta: ActorPaths.Metadata)(implicit context: ActorContext): ActorSelection =
    context.actorSelection(baseAddress.toString + actorMeta.toString())
}
