package com.github.wildprairie.common.actors.shared

import akka.actor.{Actor, ActorLogging, Props}
import com.github.wildprairie.common.actors.shared.Authenticator._

/**
  * Created by hussein on 16/05/17.
  */
object Authenticator {
  def props[TIn, TOut](f: TIn => Either[FailureReason, TOut]) : Props = {
    Props(classOf[Authenticator[TIn, TOut]], f)
  }

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

class Authenticator[TIn, TOut](f: TIn => Either[FailureReason, TOut])
  extends Actor with ActorLogging {

  def receive = {
    case Authenticate(user: TIn) =>
      f(user) match {
        case Right(output) => sender ! Success(user, output)
        case Left(reason) => sender ! Failure(user, reason)
      }
  }
}