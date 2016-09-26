package com.azavea.landsatutil

import com.github.nscala_time.time.Imports._
import akka.actor.ActorSystem
import com.typesafe.config.ConfigFactory
import geotrellis.vector._
import Json._

case class QueryResult(metadata: QueryMetadata, images: Seq[LandsatImage]) {
  def mapImages(f: Seq[LandsatImage] => Seq[LandsatImage]): QueryResult =
    QueryResult(metadata: QueryMetadata, f(images))
}

case class QueryMetadata(total: Int, skip: Int, limit: Int, lastUpdated: DateTime)

object Landsat8Query {
  val conf = ConfigFactory.load()
  val API_URL = conf.getString("landsatutil.apiUrl")

  def apply(): Landsat8Query =
    new Landsat8Query

}

class Landsat8Query() {
  private var _boundsQuery: String = ""
  private var _cloudCoverageMin = 0.0
  private var _cloudCoverageMax = 100.0
  private var _startDate = "2013-02-11"
  private var _endDate = formatDate(DateTime.now)
  private var _filterFunction: LandsatImage => Boolean = { x => true }

  private def aquisitionDate = s"acquisitionDate:[${_startDate}+TO+${_endDate}]"

  private def formatDate(dt: DateTime): String =
    DateTimeFormat.forPattern("YYYY-MM-dd").print(dt)

  def withStartDate(startDate: DateTime): Landsat8Query = {
    _startDate = formatDate(startDate)
    this
  }

  def withEndDate(endDate: DateTime): Landsat8Query = {
    _endDate = formatDate(endDate)
    this
  }

  def betweenDates(startDate: DateTime, endDate: DateTime): Landsat8Query = {
    withStartDate(startDate)
    withEndDate(endDate)
  }

  def intersects(x: Double, y: Double): Landsat8Query =
    intersects(x, y, x, y)

  def intersects(xmin: Double, ymin: Double, xmax: Double, ymax: Double): Landsat8Query =
    intersects(Extent(xmin, ymin, xmax, ymax))

  def intersects(polygon: Polygon): Landsat8Query = {
    val extent = polygon.envelope

    _boundsQuery =
      Array(
        f"lowerLeftCornerLongitude:[-1000+TO+${extent.xmax}%0,6f]",
        f"upperLeftCornerLatitude:[${extent.ymin}%0,6f+TO+1000]",
        f"lowerRightCornerLatitude:[-1000+TO+${extent.ymax}%0,6f]",
        f"upperRightCornerLongitude:[${extent.xmin}%0,6f+TO+1000]"
      ).mkString("+AND+")

    _filterFunction = { image =>
      image.footprint.intersects(polygon)
    }

    this
  }

  def contains(x: Double, y: Double): Landsat8Query =
    contains(x, y, x, y)

  def contains(xmin: Double, ymin: Double, xmax: Double, ymax: Double): Landsat8Query =
    contains(Extent(xmin, ymin, xmax, ymax))

  def contains(polygon: Polygon): Landsat8Query = {
    val extent = polygon.envelope

    _boundsQuery =
      Array(
        f"lowerLeftCornerLongitude:[-1000+TO+${extent.xmin}%0,6f]",
        f"upperLeftCornerLatitude:[${extent.ymax}%0,6f+TO+1000]",
        f"lowerRightCornerLatitude:[-1000+TO+${extent.ymin}%0,6f]",
        f"upperRightCornerLongitude:[${extent.xmax}%0,6f+TO+1000]"
      ).mkString("+AND+")

    _filterFunction = { image =>
      image.footprint.contains(polygon)
    }

    this
  }

  def withMaxCloudCoverage(maxAllowed: Double): Landsat8Query =
    withCloudCoverage(0.0, maxAllowed)

  def withCloudCoverage(min: Double, max: Double): Landsat8Query = {
    _cloudCoverageMin = min
    _cloudCoverageMax = max
    this
  }

  def searchTerms =
    Array(
      _boundsQuery,
      f"cloudCoverFull:[${_cloudCoverageMin}%0,2f+TO+${_cloudCoverageMax}%0,2f]",
      aquisitionDate
    ).mkString("+AND+")

  def execute(limit: Int = 1000, skip: Int = 0)(implicit timeout: scala.concurrent.duration.Duration): Option[QueryResult] = {
    val search = s"search=$searchTerms&limit=$limit&skip=$skip"
    val url = s"${Landsat8Query.API_URL}?$search"
    try {
      val result = HttpClient.get[QueryResult](url)
      Some(result.mapImages(_.filter(_filterFunction)))
    } catch {
      case e: spray.httpx.UnsuccessfulResponseException if e.response.status.intValue == 404 =>
        // Not found
        None
    }
  }

  def collect(): Seq[LandsatImage] = {
    implicit val timeout = scala.concurrent.duration.Duration(1000, scala.concurrent.duration.SECONDS)

    val search = s"search=$searchTerms"
    val url = s"${Landsat8Query.API_URL}?$search"
    val system = ActorSystem(s"landsat_query_collect_${java.util.UUID.randomUUID}")
    try {
      val count = HttpClient.get[QueryResult](url, system).metadata.total

      val groups = {
        val g = count / 100
        if(g * 100 == count) g else g + 1
      }

      (0 until groups)
        .map { g =>
          val skip = g * 100
          s"${Landsat8Query.API_URL}?$search&limit=100&skip=$skip"
        }
        .par
        .flatMap { url => HttpClient.get[QueryResult](url, system).images }
        .filter(_filterFunction)
        .toList
    } catch {
      case e: spray.httpx.UnsuccessfulResponseException if e.response.status.intValue == 404 =>
        // Not found
        Seq()
    } finally {
      system.shutdown()
    }

  }
}
