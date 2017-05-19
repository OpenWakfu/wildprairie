package com.github.wildprairie.common.actors.shared

import akka.actor.Actor

/**
  * Created by hussein on 16/05/17.
  */
object Authenticator {
  sealed trait Message
  final case class Authenticate[TIn](user: TIn) extends Message
  final case class Success[TIn, TOut](user: TIn, output: TOut) extends Message
  final case class Failure[TIn](user: TIn, reason: FailureReason) extends Message

  sealed trait FailureReason
  object FailureReason {
    case object WrongCredentials extends FailureReason
    case object Banned extends FailureReason
    case object AlreadyConnected extends FailureReason
    case object UnknownException extends FailureReason
  }
}

trait Authenticator[TIn, TOut] {
  self: Actor =>
}