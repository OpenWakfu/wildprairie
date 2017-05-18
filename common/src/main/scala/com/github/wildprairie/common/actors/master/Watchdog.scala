package com.github.wildprairie.common.actors.master

import akka.actor.Props
import com.github.wildprairie.common.actors.ClusteredActor

/**
  * Created by hussein on 16/05/17.
  */
object Watchdog {
  def props(): Props =
    Props(classOf[Watchdog])
}

final class Watchdog extends ClusteredActor {
  // TODO: define metrics handling and servers coordination
  override def receive: Receive = {
    case _ => Unit
  }
}
