package com.github.wildprairie.auth

import com.github.wildprairie.common.ClusterSystem
import com.github.wildprairie.common.actors.auth.{AccountAuthenticator, AuthServer}

import scala.concurrent.Await
import scala.concurrent.duration.Duration

/**
  * Created by hussein on 15/05/17.
  */
object Main {
  def main(args: Array[String]): Unit = {
    val system = ClusterSystem.start()
    val authenticator = system.actorOf(AccountAuthenticator.props(), "authenticator")
    val server = system.actorOf(AuthServer.props(authenticator), "auth-server")
    Await.result(system.whenTerminated, Duration.Inf)
  }
}
