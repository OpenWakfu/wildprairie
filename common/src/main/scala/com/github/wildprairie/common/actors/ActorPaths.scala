package com.github.wildprairie.common.actors

/**
  * Created by hussein on 19/05/17.
  */
object ActorPaths {

  object Auth {
    final object AuthServer extends Metadata("auth-server")
    final object Authenticator extends Metadata("authenticator")
  }

  object World {
    final object WorldServer extends Metadata("world-server")
    final object Authenticator extends Metadata("authenticator")
    final object CharacterIdSupplier extends Metadata("character-id-supplier", Some(WorldServer))
  }

  object Metadata {
    val ROOT_PARENT = "/user"
  }

  class Metadata(val name: String, parentOpt: Option[Metadata] = None) {
    override def toString: String = {
      parentOpt match {
        case Some(parent) => parent.toString()
        case None => Metadata.ROOT_PARENT
      }
    } + s"/$name"
  }
}
