package com.github.wildprairie.common.model

import io.getquill.{PostgresAsyncContext, SnakeCase}

/**
  * Created by jacek on 19.05.17.
  */
object Database {
  val context = new PostgresAsyncContext[SnakeCase]("db")
}
