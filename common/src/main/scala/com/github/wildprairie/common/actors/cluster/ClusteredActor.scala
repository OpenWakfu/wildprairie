package com.github.wildprairie.common.actors.cluster

import akka.actor.Actor
import akka.cluster.Cluster
import akka.cluster.ClusterEvent._

/**
  * Created by hussein on 15/05/17.
  */
abstract class ClusteredActor extends Actor {

  val cluster = Cluster(context.system)

  override def preStart(): Unit =
    cluster.subscribe(self, classOf[MemberEvent], classOf[UnreachableMember])

  override def postStop(): Unit =
    cluster.unsubscribe(self)
}
