package com.azavea.landsatutil

import org.scalatest._
import com.github.nscala_time.time.Imports._
import geotrellis.vector._
import geotrellis.vector.io.json._

class Landsat8QuerySpec extends FunSpec with Matchers {
  def getJson(resource: String): String = {
    val stream = getClass.getResourceAsStream(resource)
    val lines = scala.io.Source.fromInputStream(stream).getLines
    val json = lines.mkString(" ")
    stream.close()
    json
  }

  describe("Landsat8Query") {
    it("should find landsat image that contains Philly") {
      val philly = getJson("/philly.json").parseGeoJson[Polygon]
      val images =
        Landsat8Query()
          .withStartDate(new DateTime(2015, 8, 10, 0, 0, 0))
          .withEndDate(new DateTime(2015, 8, 10, 0, 0, 0))
          .contains(philly)
          .collect()

      images.size should be (1)
      images.head.sceneId should be ("LC80140322015222LGN00")
    }
  }
}
