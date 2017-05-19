package com.github.wildprairie.world

import com.github.wildprairie.common.ClusterSystem
import com.github.wildprairie.common.actors.world.{TokenAuthenticator, WorldServer}

import scala.concurrent.Await
import scala.concurrent.duration.Duration

/**
  * Created by hussein on 16/05/17.
  */
object Main {
  def main(args: Array[String]): Unit = {
    val system = ClusterSystem.start()
    val authenticator = system.actorOf(TokenAuthenticator.props(), "authenticator")
    val server = system.actorOf(WorldServer.props(authenticator), "world-server")
    Await.result(system.whenTerminated, Duration.Inf)
  }
}
