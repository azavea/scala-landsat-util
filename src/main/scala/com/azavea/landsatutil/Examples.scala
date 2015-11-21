package com.azavea.landsatutil

import com.github.nscala_time.time.Imports._
import spray.json._
import scala.collection.mutable
import akka.actor.ActorSystem
import com.typesafe.config.ConfigFactory
import geotrellis.vector._
import geotrellis.vector.io.json._
import Json._

object Examples {
  def main(args: Array[String]): Unit =
    phillyExample()

  def phillyExample(): Unit = {
    val philly = GeoJson.fromFile[Polygon]("src/test/resources/philly.json")

    val images =
      Landsat8Query()
        .withStartDate(new DateTime(2012, 1, 12, 0, 0, 0))
        .withEndDate(new DateTime(2015, 11, 5, 0, 0, 0))
        .withMaxCloudCoverage(80.0)
        .intersects(philly)
        .collect()

    val s3Client = S3Client()
    val filtered =
      images
        .filter(s3Client.imageExists(_))

    filtered
      .foreach { image => 
        println(s"${image.sceneId} - ${image.cloudPercentage}% clouds")
        println(s"\tAquisition Date: ${image.aquisitionDate}")
        println(s"\tUSGS Thumbnail URL: ${image.thumbnailUrl}")
        println(s"\tAWS large thumbnail URL: ${image.largeThumbnail}")
        println(s"\tGoogle URL: ${image.googleUrl}")
        println(s"\tFootprint GeoJSON: ${image.footprint.toGeoJson}")
      }

    println(s"Results: ${filtered.size} images.")
  }

  def intersectExample(): Unit = {
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
