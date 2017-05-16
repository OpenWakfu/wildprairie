package com.github.wildprairie.common.cluster

import akka.actor.{Actor, ActorSystem, Props}
import akka.cluster.Cluster
import akka.cluster.ClusterEvent._

import scala.reflect.ClassTag

/**
  * Created by hussein on 15/05/17.
  */
object ClusterService {
  val SYSTEM_NAME = "wildprairie"

  def startSystem[T <: ClusterService : ClassTag]() : ActorSystem = {
    val system = ActorSystem(SYSTEM_NAME)
    system.actorOf(Props[T])
    system
  }
}

abstract class ClusterService extends Actor {

  val cluster = Cluster(context.system)

  override def preStart(): Unit =
    cluster.subscribe(self, classOf[MemberEvent], classOf[UnreachableMember])

  override def postStop(): Unit =
    cluster.unsubscribe(self)
}
