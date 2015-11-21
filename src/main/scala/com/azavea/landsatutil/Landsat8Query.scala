package com.azavea.landsatutil

import com.github.nscala_time.time.Imports._
import spray.json._
import scala.collection.mutable
import akka.actor.ActorSystem
import com.typesafe.config.ConfigFactory
import Json._

object Landsat8 {
  def main(args: Array[String]): Unit = {
    val images =
      Landsat8Query()
        .withStartDate(new DateTime(2015, 1, 12, 0, 0, 0))
        .withEndDate(new DateTime(2015, 11, 5, 0, 0, 0))
//        .withBounds(3.33984375,55.537956716301615,32.16796875,71.27788499668304)
//        .intersects(18.17138671875,69.07322347622011,20.390625,69.76826903596766)
//        .contains(18.17138671875,69.07322347622011,20.390625,69.76826903596766)
        .contains(-75.26596069335938,39.88296828403436,-75.05859375,40.01351528489102)
//        .contains(-75.2783203125,39.88102530755859,-74.99267578125,40.09146778591135)
//        .intersects(-80.61767578124999,39.598810498772686,-74.64111328125,42.238176986875544)
        .collect()

    images.filter(_.cloudPercentage < 10).take(5).map { image =>
      println(s"${image.sceneId} - ${image.cloudPercentage}% clouds")
      println(image.thumbnailUrl)
      println(image.largeThumbnail)
      println(image.smallThumbnail)
      println(image.footprint)
      println(s"${image.lowerLeft._1}, ${image.lowerRight._2}, ${image.upperRight._1}, ${image.upperLeft._2}")
        // f"lowerLeftCornerLongitude:[-1000+TO+$xmin%0,5f]",
        // f"upperLeftCornerLatitude:[$ymax%0,5f+TO+1000]",
        // f"lowerRightCornerLatitude:[-1000+TO+$ymin%0,5f]",
        // f"upperRightCornerLongitude:[$xmax%0,5f+TO+1000]"

    }

    println(s"Results: ${images.size} images.")
  }

  def main2(args: Array[String]): Unit = {
    implicit val d = scala.concurrent.duration.Duration(30, scala.concurrent.duration.SECONDS)

    val result =
      Landsat8Query()
        .intersects(-75.65185546874999,39.69701710019832,-74.718017578125,40.24009510908543)
        .execute()

    result match {
      case Some(r) =>
        for(image <- r.images.take(1)) {
          println(image.thumbnailUrl)
          println(image.largeThumbnail)
          println(image.smallThumbnail)
        }
        println(s"RESULT COUNT: ${r.metadata.total}")
      case None =>
        println("No results found!")
    }

  }
}

object Landsat8Query {
  val conf = ConfigFactory.load()
  val API_URL = conf.getString("landsatutil.apiUrl")

  def apply(): Landsat8Query =
    new Landsat8Query

}

class Landsat8Query() {
  private var _boundsQuery: String = ""
  private var _cloudCover = "cloudCoverFull:[0+TO+100]"
  private var _startDate = "2013-02-11"
  private var _endDate = formatDate(DateTime.now)

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

  def intersects(xmin: Double, ymin: Double, xmax: Double, ymax: Double): Landsat8Query = {
    _boundsQuery =
      Array(
        f"lowerLeftCornerLongitude:[-1000+TO+$xmax%0,5f]",
        f"upperLeftCornerLatitude:[$ymin%0,5f+TO+1000]",
        f"lowerRightCornerLatitude:[-1000+TO+$ymax%0,5f]",
        f"upperRightCornerLongitude:[$xmin%0,5f+TO+1000]"
      ).mkString("+AND+")

    this
  }

  def contains(x: Double, y: Double): Landsat8Query =
    contains(x, y, x, y)

  def contains(xmin: Double, ymin: Double, xmax: Double, ymax: Double): Landsat8Query = {
    _boundsQuery =
      Array(
        f"lowerLeftCornerLongitude:[-1000+TO+$xmin%0,5f]",
        f"upperLeftCornerLatitude:[$ymax%0,5f+TO+1000]",
        f"lowerRightCornerLatitude:[-1000+TO+$ymin%0,5f]",
        f"upperRightCornerLongitude:[$xmax%0,5f+TO+1000]"
      ).mkString("+AND+")

    this
  }

  def withCloudCoverage(min: Double, max: Double): Landsat8Query = {
    _cloudCover = f"cloudCoverFull:[$min%0,2f+TO+$max%0,2f]"
    this
  }

  def searchTerms =
    Array(
      _boundsQuery,
      _cloudCover,
      aquisitionDate
    ).mkString("+AND+")

  def execute(limit: Int = 1000, skip: Int = 0)(implicit timeout: scala.concurrent.duration.Duration): Option[QueryResult] = {
    val search = s"search=$searchTerms&limit=$limit&skip=$skip"
    val url = s"${Landsat8Query.API_URL}?$search"
    try {
      Some(Client.get[QueryResult](url))
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
      val count = Client.get[QueryResult](url, system).metadata.total

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
        .flatMap { url => println(url) ; Client.get[QueryResult](url, system).images }
        .toList
    } catch {
      case e: spray.httpx.UnsuccessfulResponseException if e.response.status.intValue == 404 =>
        // Not found
        Seq()
    } finally {
      println("SHUTDOWN")
      system.shutdown()
    }

  }
}
