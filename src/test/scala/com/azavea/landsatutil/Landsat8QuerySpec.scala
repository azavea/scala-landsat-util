package com.azavea.landsatutil

import org.scalatest._
import geotrellis.vector._
import geotrellis.vector.io._
import java.time.{LocalDate, ZoneOffset, ZonedDateTime}

import scala.util.matching.Regex
import scala.util.{Failure, Success}

class Landsat8QuerySpec extends FunSpec with Matchers {

  describe("Landsat8Query") {
    it("should find landsat image that contains Philly") {
      val philly = Resource.string("/philly.json").parseGeoJson[Polygon]
      val images =
        Landsat8Query()
          .withStartDate(ZonedDateTime.of(2015, 8, 10, 0, 0, 0, 0, ZoneOffset.UTC))
          .withEndDate(ZonedDateTime.of(2015, 8, 10, 0, 0, 0, 0, ZoneOffset.UTC))
          .contains(philly)
          .collect() match {
            case Success(r) => r
            case Failure(e) => throw e
          }

      images.size should be (1)
      images.head.sceneId should be ("LC80140322015222LGN01")
    }

    it ("should produce only well formatted queries") {
      val dateTime = LocalDate.of(2014, 10, 22).atStartOfDay(ZoneOffset.UTC)

      // The coordinates are only used to produce a query string.
      // It is not important whether the resource exists.
      case object coordinates {
        val xmin = 20.60731
        val ymin = 85.85002
        val xmax = 22.71835
        val ymax = 88.06044
      }

      val query1 = Landsat8Query()
        .withStartDate(dateTime)
        .withEndDate(dateTime.plusDays(1))

      val query2 = query1
        .contains(coordinates.xmin, coordinates.ymin, coordinates.xmax, coordinates.ymax)

      val query3 = query1
        .intersects(coordinates.xmin, coordinates.ymin, coordinates.xmax, coordinates.ymax)

      val query4 = Landsat8Query().withCloudCoverage(100.42, 203.76)

      val query5 = Landsat8Query()
        .contains(coordinates.xmin, coordinates.ymin, coordinates.xmax, coordinates.ymax)

      // Queries begin with a letter and do not contain commas
      def formatCheckOf(query: Landsat8Query): Boolean = {
        val pattern: Regex = "^[a-zA-Z][^,]*".r
        def check(regex: Regex, string: String): Boolean = string match {
          case regex(_*) => true
          case _ => false
        }
        check(pattern, query.searchTerms)
      }

      formatCheckOf(query1) should be (true)
      formatCheckOf(query2) should be (true)
      formatCheckOf(query3) should be (true)
      formatCheckOf(query4) should be (true)
      formatCheckOf(query5) should be (true)
    }
  }
}
