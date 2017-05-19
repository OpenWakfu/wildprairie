package com.github.wildprairie.common.actors.cluster

import akka.actor.{ActorSystem, Props}

import scala.reflect.ClassTag

/**
  * Created by hussein on 16/05/17.
  */
object ClusterSystem {
  val SYSTEM_NAME = "wildprairie"

  def start[T <: ClusteredActor: ClassTag](): ActorSystem = {
    val system = ActorSystem(SYSTEM_NAME)
    system.actorOf(Props[T])
    system
  }
}
