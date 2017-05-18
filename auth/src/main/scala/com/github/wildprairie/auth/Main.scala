package com.github.wildprairie.auth

import com.github.wakfutcp.protocol.common.Community
import com.github.wakfutcp.protocol.messages.forClient.AccountInformation
import com.github.wakfutcp.protocol.messages.forServer.ClientDispatchAuthenticationMessage
import com.github.wakfutcp.protocol.protobuf.account.Status
import com.github.wildprairie.common.ClusterSystem
import com.github.wildprairie.common.actors.auth.AuthServer
import com.github.wildprairie.common.actors.common.Authenticator

import scala.concurrent.Await
import scala.concurrent.duration.Duration

/**
  * Created by hussein on 15/05/17.
  */
object Main {
  def main(args: Array[String]): Unit = {
    val system = ClusterSystem.start()
    val authenticator = system.actorOf(Authenticator.props((_: ClientDispatchAuthenticationMessage.CredentialData) => {
      Right(AccountInformation(Community.FR, None, Status(Map())))
    }), "authenticator")
    val server = system.actorOf(AuthServer.props(authenticator), "auth-server")
    Await.result(system.whenTerminated, Duration.Inf)
  }
}
