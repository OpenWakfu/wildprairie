package com.github.wildprairie.common.actors

import akka.actor.{Actor, ActorLogging}
import akka.cluster.Cluster
import akka.cluster.ClusterEvent._

/**
  * Created by hussein on 15/05/17.
  */
abstract class ClusteredActor extends Actor with ActorLogging {
  protected val cluster = Cluster(context.system)

  override def preStart(): Unit =
    cluster.subscribe(
      self,
      initialStateAsEvents,
      classOf[MemberEvent],
      classOf[UnreachableMember]
    )

  override def postStop(): Unit =
    cluster.unsubscribe(self)
}
