package com.github.wildprairie.common.actors.shared

import com.github.wakfutcp.protocol.common.{Proxy, WorldInfo}

/**
  * Created by hussein on 18/05/17.
  */
final class WorldServerSpec(val info: WorldInfo, val proxy: Proxy) extends Serializable
