package com.azavea.landsatutil

import akka.actor.ActorSystem
import com.typesafe.config.ConfigFactory
import geotrellis.vector._
import Json._

import java.time.{ZoneOffset, ZonedDateTime}
import java.time.format.DateTimeFormatter
import java.util.Locale

case class QueryResult(metadata: QueryMetadata, images: Seq[LandsatImage]) {
  def mapImages(f: Seq[LandsatImage] => Seq[LandsatImage]): QueryResult =
    QueryResult(metadata: QueryMetadata, f(images))
}

case class QueryMetadata(total: Int, skip: Int, limit: Int, lastUpdated: ZonedDateTime)

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
  private var _endDate = formatDate(ZonedDateTime.now)
  private var _filterFunction: LandsatImage => Boolean = { x => true }

  private def aquisitionDate = s"acquisitionDate:[${_startDate}+TO+${_endDate}]"

  private def formatDate(dt: ZonedDateTime): String =
    DateTimeFormatter.ofPattern("yyyy-MM-dd").withZone(ZoneOffset.UTC).format(dt)

  private def doubleString(double: Double, fractionDigits: Int): String =
    s"%.${fractionDigits}f".formatLocal(Locale.ENGLISH, double)

  def withStartDate(startDate: ZonedDateTime): Landsat8Query = {
    _startDate = formatDate(startDate)
    this
  }

  def withEndDate(endDate: ZonedDateTime): Landsat8Query = {
    _endDate = formatDate(endDate)
    this
  }

  def betweenDates(startDate: ZonedDateTime, endDate: ZonedDateTime): Landsat8Query = {
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
        s"lowerLeftCornerLongitude:[-1000+TO+${doubleString(extent.xmax, 6)}]",
        s"upperLeftCornerLatitude:[${doubleString(extent.ymin, 6)}+TO+1000]",
        s"lowerRightCornerLatitude:[-1000+TO+${doubleString(extent.ymax, 6)}]",
        s"upperRightCornerLongitude:[${doubleString(extent.xmin, 6)}+TO+1000]"
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
        s"lowerLeftCornerLongitude:[-1000+TO+${doubleString(extent.xmin, 6)}]",
        s"upperLeftCornerLatitude:[${doubleString(extent.ymax, 6)}+TO+1000]",
        s"lowerRightCornerLatitude:[-1000+TO+${doubleString(extent.ymin, 6)}]",
        s"upperRightCornerLongitude:[${doubleString(extent.xmax, 6)}+TO+1000]"
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
      s"cloudCoverFull:[${doubleString(_cloudCoverageMin, 2)}+TO+${doubleString(_cloudCoverageMax, 2)}]",
      aquisitionDate
    ).filter(!_.isEmpty).mkString("+AND+")

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
      system.terminate()
    }

  }
}
