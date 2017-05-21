package com.github.wildprairie.common.actors.shared

import akka.actor.{Actor, ActorRef, Stash}

import scala.concurrent.{Future, Promise}

/**
  * Created by hussein on 21/05/17.
  */
trait ActorFolder { this: Actor with Stash =>

  def fold[B](actors: Traversable[ActorRef], input: Any)(accumulator: B)(
    accumulate: PartialFunction[(Any, B), B]): Future[B] = {
    val promise = Promise[B]()
    if (actors.isEmpty) {
      promise.success(accumulator)
    } else {
      actors.foreach(_ ! input)
      context.become(collectAndDispatch(actors.size, promise)(accumulator)(accumulate))
    }
    promise.future
  }

  private[this] def collectAndDispatch[B](remaining: Int, promise: Promise[B])(accumulator: B)(
    accumulate: PartialFunction[(Any, B), B]): Receive = {
    case msg =>
      val matched =
        accumulate.runWith { res =>
          if (remaining > 1) {
            context.become(
              collectAndDispatch(remaining - 1, promise)(res)(accumulate),
              discardOld = true
            )
          } else {
            promise.success(res)
            unstashAll()
            context.unbecome()
          }
        }.apply((msg, accumulator))

      if (!matched) stash()
  }
}
