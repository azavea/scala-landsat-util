package com.azavea.landsatutil

import java.time.{ZoneOffset, ZonedDateTime, LocalDate}
import java.time.format.DateTimeFormatter

import spray.json._
import geotrellis.vector._

object Json {
  def parseDate(s: String) =
    LocalDate.from(DateTimeFormatter.ofPattern("yyyy-MM-dd").parse(s)).atStartOfDay(ZoneOffset.UTC)

  def parseTime(s: String) =
    ZonedDateTime.from(DateTimeFormatter.ofPattern("yyyy:DDD:HH:mm:ss.SSSSSSS").withZone(ZoneOffset.UTC).parse(s))

  implicit object QueryMetadataFormat extends RootJsonReader[QueryMetadata] {
    def read(json: JsValue): QueryMetadata =
      json.asJsObject.getFields("last_updated", "results") match {
        case Seq(JsString(lastUpdated), results) =>
          results.asJsObject.getFields("skip", "limit", "total") match {
            case Seq(JsNumber(skip), JsNumber(limit), JsNumber(total)) =>
              QueryMetadata(total.toInt, skip.toInt, limit.toInt, parseDate(lastUpdated))
            case _ =>
              throw new DeserializationException("QueryMetadata expected.")
          }
        case _ =>
          throw new DeserializationException("QueryMetadata expected.")
      }
  }

  implicit object LandsatImageFormat extends RootJsonReader[LandsatImage] {
    def read(json: JsValue): LandsatImage = {
      val fields = json.asJsObject.fields

      def getString(field: String): String =
        fields.get(field) match {
          case Some(jv) =>
            jv match {
              case JsString(s) => s
              case _ =>
                throw new DeserializationException(s"Expected field $field to be a string value.")
            }
          case None =>
            throw new DeserializationException(s"Expected field $field in image data.")
        }

      def getNumber(field: String): BigDecimal =
        fields.get(field) match {
          case Some(jv) =>
            jv match {
              case JsNumber(n) => n
              case _ =>
                throw new DeserializationException(s"Expected field $field to be a number value.")
            }
          case None =>
            throw new DeserializationException(s"Expected field $field in image data.")
        }

      val sceneId = getString("sceneID")
      val row = getNumber("row")
      val path = getNumber("path")
      val aquisitionDate = getString("acquisitionDate")
      val cloudPercentage = getNumber("cloudCoverFull")
      val thumbnailUrl = getString("browseURL")
      val lowerLeftCornerLongitude = getNumber("lowerLeftCornerLongitude")
      val lowerLeftCornerLatitude = getNumber("lowerLeftCornerLatitude")
      val upperRightCornerLongitude = getNumber("upperRightCornerLongitude")
      val upperRightCornerLatitude = getNumber("upperRightCornerLatitude")
      val lowerRightCornerLongitude = getNumber("lowerRightCornerLongitude")
      val lowerRightCornerLatitude = getNumber("lowerRightCornerLatitude")
      val upperLeftCornerLongitude = getNumber("upperLeftCornerLongitude")
      val upperLeftCornerLatitude = getNumber("upperLeftCornerLatitude")
      val sceneStartTime = getString("sceneStartTime")
      val sceneEndTime = getString("sceneStopTime")
      val imageQuality = getNumber("imageQuality1")
      val sunAzimuth = getNumber("sunAzimuth")
      val sunElevation = getNumber("sunElevation")
      val dayOrNight = getString("dayOrNight")
      val sensor = getString("sensor")
      val receivingStation = getString("receivingStation")
      val dateUpdated = getString("dateUpdated")

      LandsatImage(
        sceneId = sceneId,
        satellite = "L8",
        row = row.toInt,
        path = path.toInt,
        aquisitionDate = parseDate(aquisitionDate),
        cloudPercentage = cloudPercentage.toDouble,
        thumbnailUrl = thumbnailUrl,
        lowerLeft = Point(lowerLeftCornerLongitude.toDouble, lowerLeftCornerLatitude.toDouble),
        lowerRight = Point(lowerRightCornerLongitude.toDouble, lowerRightCornerLatitude.toDouble),
        upperLeft = Point(upperLeftCornerLongitude.toDouble, upperLeftCornerLatitude.toDouble),
        upperRight = Point(upperRightCornerLongitude.toDouble, upperRightCornerLatitude.toDouble),
        sceneStartTime = parseTime(sceneStartTime),
        sceneEndTime = parseTime(sceneEndTime),
        imageQuality = imageQuality.toInt,
        sunAzmith = sunAzimuth.toDouble,
        sunElevation = sunElevation.toDouble,
        dayTime = dayOrNight == "DAY",
        sensor = sensor,
        receivingStation = receivingStation,
        dateUpdated = parseDate(dateUpdated)
      )
    }
  }

  implicit object QueryResultFormat extends RootJsonReader[QueryResult] {
    def read(json: JsValue): QueryResult =
      json.asJsObject.getFields("meta", "results") match {
        case Seq(meta, JsArray(results)) =>
          QueryResult(
            meta.convertTo[QueryMetadata],
            results.map(_.convertTo[LandsatImage])
          )
        case _ =>
          throw new DeserializationException("QueryResult expected. Received: " + json)
      }
  }

}
