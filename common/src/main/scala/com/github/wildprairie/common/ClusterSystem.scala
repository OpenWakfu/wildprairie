package com.github.wildprairie.common

import akka.actor.ActorSystem

/**
  * Created by hussein on 16/05/17.
  */
object ClusterSystem {
  val SYSTEM_NAME = "wildprairie"

  def start() : ActorSystem = ActorSystem(SYSTEM_NAME)
}
