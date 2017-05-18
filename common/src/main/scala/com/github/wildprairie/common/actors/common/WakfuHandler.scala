package com.github.wildprairie.common.actors.common

import akka.actor.{Actor, ActorLogging, ActorRef}
import com.github.wakfutcp.traits.StatefulActor

/**
  * Created by hussein on 16/05/17.
  */
abstract class WakfuHandler(server: ActorRef, authenticator: ActorRef)
  extends Actor with StatefulActor with ActorLogging
