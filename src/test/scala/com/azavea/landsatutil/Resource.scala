package com.azavea.landsatutil

import scala.io.Source

object Resource {
  def stream(name: String) = {
    getClass.getResourceAsStream(name)
  }

  def string(name: String) = {
   Source.fromInputStream(stream(name)).getLines.mkString
  }
}
