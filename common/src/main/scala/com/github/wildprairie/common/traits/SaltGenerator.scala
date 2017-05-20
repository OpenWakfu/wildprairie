package com.github.wildprairie.common.traits

import scala.util.Random

/**
  * Created by hussein on 20/05/17.
  */
trait SaltGenerator {
  private val random = new Random()

  def nextSalt: Long = random.nextLong()
}
