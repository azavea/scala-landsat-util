package com.azavea.landsatutil

import org.scalatest._
import geotrellis.vector._
import geotrellis.vector.io._

import java.time.{ZonedDateTime, ZoneOffset}

class Landsat8QuerySpec extends FunSpec with Matchers {

  describe("Landsat8Query") {
    it("should find landsat image that contains Philly") {
      val philly = Resource.string("/philly.json").parseGeoJson[Polygon]
      val images =
        Landsat8Query()
          .withStartDate(ZonedDateTime.of(2015, 8, 10, 0, 0, 0, 0, ZoneOffset.UTC))
          .withEndDate(ZonedDateTime.of(2015, 8, 10, 0, 0, 0, 0, ZoneOffset.UTC))
          .contains(philly)
          .collect()

      images.size should be (1)
      images.head.sceneId should be ("LC80140322015222LGN00")
    }
  }
}
