package com.github.wildprairie.master

import com.github.wildprairie.common.ClusterSystem
import com.github.wildprairie.common.actors.master.Watchdog

import scala.concurrent.Await
import scala.concurrent.duration.Duration

/**
  * Created by hussein on 15/05/17.
  */
object Main {
  def main(args: Array[String]): Unit = {
    val system = ClusterSystem.start()
    val watchdog = system.actorOf(Watchdog.props())
    Await.result(system.whenTerminated, Duration.Inf)
  }
}
