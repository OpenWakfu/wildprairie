package com.github.wildprairie.common.utils

import java.nio.file.Paths
import java.security.{PrivateKey, PublicKey}

/**
  * Created by hussein on 18/05/17.
  */
object Cipher {
  val RSA = new Cipher("RSA")
}

class Cipher(algorithm: String) {
  import java.nio.file.Files
  import java.security.KeyFactory
  import java.security.spec.X509EncodedKeySpec
  import java.security.spec.PKCS8EncodedKeySpec

  def generatePrivate: PrivateKey = {
    val keyBytes = Files.readAllBytes(Paths.get(getClass.getResource("/private_key.der").toURI))
    val spec = new PKCS8EncodedKeySpec(keyBytes)
    val kf = KeyFactory.getInstance(algorithm)
    kf.generatePrivate(spec)
  }

  def generatePublic: PublicKey = {
    val keyBytes = Files.readAllBytes(Paths.get(getClass.getResource("/public_key.der").toURI))
    val spec = new X509EncodedKeySpec(keyBytes)
    val kf = KeyFactory.getInstance(algorithm)
    kf.generatePublic(spec)
  }

  def decrypt(privateKey: PrivateKey, input: Array[Byte]): Array[Byte] = {
    val decrypt = javax.crypto.Cipher.getInstance(algorithm)
    decrypt.init(javax.crypto.Cipher.DECRYPT_MODE, privateKey)
    decrypt.doFinal(input)
  }
}
