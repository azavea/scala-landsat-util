package com.azavea.landsatutil

import java.io.InputStream

import scala.io.Source

object Resource {
  def stream(name: String): InputStream = {
    getClass.getResourceAsStream(name)
  }

  def string(name: String): String = {
   Source.fromInputStream(stream(name)).getLines.mkString
  }
}
