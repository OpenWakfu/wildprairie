package com.github.wildprairie.common.actors.shared

import akka.actor.{Actor, ActorRef, Stash}

/**
  * Created by hussein on 21/05/17.
  */
object Aggregator {
  sealed trait Message
  final case class Result[TIn, TOut](input: TIn, output: List[TOut]) extends Message
}

trait Aggregator[TIn, TOut] {
  this : Actor with Stash =>

  import Aggregator._

  // TODO: add timeout
  def aggregate(input: TIn, actors: List[ActorRef]): Unit = {
    actors.foreach(_ ! input)
    context.become(handleAggregateResult(input, actors, List()))
  }

  def handleAggregateResult(input: TIn, actors: List[ActorRef], output: List[TOut]) : Receive = {
    case message: TOut if actors.contains(sender) =>
      val newOutput = message :: output
      if(newOutput.length == actors.length) {
        // last result
        self ! Result(input, newOutput)
        unstashAll()
        context.unbecome()
      }
      else {
        context.become(handleAggregateResult(input, actors, newOutput), true)
      }

    case _ =>
      stash()
  }
}
