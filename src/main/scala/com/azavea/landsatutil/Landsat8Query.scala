package com.azavea.landsatutil

import java.time.format.DateTimeFormatter
import java.time.{ZoneOffset, ZonedDateTime}
import java.util.Locale

import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport
import akka.http.scaladsl.model.Uri
import com.azavea.landsatutil.Json._
import com.typesafe.config.{Config, ConfigFactory}
import com.typesafe.scalalogging.LazyLogging
import geotrellis.vector._

import java.time.{ZoneOffset, ZonedDateTime}
import java.time.format.DateTimeFormatter
import java.util.Locale
import scala.concurrent.{Await, Future}
import scala.util.Try

case class QueryResult(metadata: QueryMetadata, images: Seq[LandsatImage]) {
  def mapImages(f: Seq[LandsatImage] => Seq[LandsatImage]): QueryResult =
    QueryResult(metadata: QueryMetadata, f(images))
}

case class QueryMetadata(found: Int, page: Int, limit: Int)

object Landsat8Query {
  val conf: Config = ConfigFactory.load()
  val API_URL: String = conf.getString("landsatutil.apiUrl")
  val ExecuteQueryLimit = 1000
  val CollectQueryLimit = 100

  def apply() = new Landsat8Query()
}

class Landsat8Query() extends SprayJsonSupport with LazyLogging {


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

  def searchTerms: String =
    Array(
      _boundsQuery,
      s"cloudCoverFull:[${doubleString(_cloudCoverageMin, 2)}+TO+${doubleString(_cloudCoverageMax, 2)}]",
      aquisitionDate
    ).filter(!_.isEmpty).mkString("+AND+")

  def execute(limit: Int = Landsat8Query.ExecuteQueryLimit, page: Int = 1)(implicit timeout: scala.concurrent.duration.Duration): Try[QueryResult] = {
    val query = Uri.Query(s"search=$searchTerms&limit=$limit&page=$page")
    val client = new HttpClient(Landsat8Query.API_URL)
    // Imports execution context for `Future`s to run in.
    import client.system.dispatcher

    try {
      val result = client.get[QueryResult](query)
        .map(_.mapImages(_.filter(_filterFunction)))
      Try(Await.result(result, timeout))
    }
    finally {
      client.shutdown()
    }
  }

  def collect(terminateAkka: Boolean = true): Try[Seq[LandsatImage]] = {
    implicit val timeout = scala.concurrent.duration.Duration(1000, scala.concurrent.duration.SECONDS)

    val termSearch = s"search=$searchTerms"

    def numGroups(total: Int): Int = {
      val g = total / 100
      if (g * 100 == total) g else g + 1
    }

    def queryGroup(groupNum: Int) = {
      val skip = groupNum * 100
      Uri.Query(s"$termSearch&limit=100&skip=$skip")
    }

    val client = new HttpClient(Landsat8Query.API_URL)
    // Imports execution context for `Future`s to run in.
    import client.system.dispatcher

    Try {
      try {
        // TODO: Make use of Host-level connection pool
        val parallelRequests =
          client.get[QueryResult](Uri.Query(termSearch))
            .flatMap(query ⇒ {
              val groups = numGroups(query.metadata.found)
              val groupImages =
                for (g ← 0 until groups) yield {
                  client
                    .get[QueryResult](queryGroup(g))
                    .map(_.images.filter(_filterFunction))
                }
              Future.sequence(groupImages).map(_.flatten)
            })

        // Fork-join pattern...
        Await.result(parallelRequests, timeout)
      } finally {
        client.shutdown()
      }
    }
  }
}
