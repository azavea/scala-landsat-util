package com.azavea.landsatutil

import geotrellis.vector._
import geotrellis.raster._
import geotrellis.raster.io.geotiff.reader._
import com.amazonaws.services.s3._
import com.amazonaws.auth._
import com.amazonaws.services.s3.model._
import org.apache.commons.io.IOUtils
import com.typesafe.scalalogging.LazyLogging

import java.net._
import java.time.{ZonedDateTime, ZoneOffset}

case class LandsatImage(
  sceneId: String,
  satellite: String,
  row: Int,
  path: Int,
  aquisitionDate: ZonedDateTime,
  cloudPercentage: Double,
  thumbnailUrl: String,
  lowerLeft: Point,
  lowerRight: Point,
  upperLeft: Point,
  upperRight: Point,
  sceneStartTime: ZonedDateTime,
  sceneEndTime: ZonedDateTime,
  imageQuality: Int,
  sunAzmith: Double,
  sunElevation: Double,
  dayTime: Boolean,
  sensor: String,
  receivingStation: String,
  dateUpdated: ZonedDateTime
) extends LazyLogging {
  def baseS3Path = f"L8/$path%03d/$row%03d/$sceneId"
  def rootUri = s"s3://landsat-pds/$baseS3Path"
  def bandUri(band: Int) = s"s3://landsat-pds/$baseS3Path/${sceneId}_B${band}.TIF"
  def bandUri(band: String) = s"s3://landsat-pds/$baseS3Path/${sceneId}_B${band.trim.toUpperCase}.TIF"
  def qaBandUri = s"s3://landsat-pds/$baseS3Path/${sceneId}_BQA.TIF"
  def mtlUri = s"s3://landsat-pds/$baseS3Path/${sceneId}_MTL.txt"
  def largeThumbnail = s"https://landsat-pds.s3.amazonaws.com/$baseS3Path/${sceneId}_thumb_large.jpg"
  def smallThumbnail = s"https://landsat-pds.s3.amazonaws.com/$baseS3Path/${sceneId}_thumb_small.jpg"
  def googleUrl = s"http://storage.googleapis.com/earthengine-public/landsat/${baseS3Path}.tar.bz"

  def footprint: Polygon =
    Polygon(Line(upperLeft, upperRight, lowerRight, lowerLeft, upperLeft))

  def getFromGoogle(bandsWanted: Seq[String], hook: IOHook = IOHook.passthrough): LandsatRaster = {
    val url = new URL(googleUrl)
    logger.info(s"Getting $sceneId from '$url'")
    LandsatRaster.fromTarBz(hook(this, "all.tar.bz", url.openStream), bandsWanted)
  }

  def imageExistsS3(s3client: AmazonS3Client = S3Client.default): Boolean = {
    import scala.collection.JavaConverters._
    s3client
      .listObjects(new ListObjectsRequest("landsat-pds", baseS3Path, null, null, null))
      .getObjectSummaries
      .size > 0
  }

  def getRasterFromS3(
    bandsWanted: Seq[String],
    s3client: AmazonS3Client = S3Client.default,
    hook: IOHook = IOHook.passthrough
  ): ProjectedRaster[MultibandTile] = {
    val tifs =
      for (band <- bandsWanted) yield {
        val uri = new URI(bandUri(band))
        logger.info(s"Getting $sceneId $band band from '$uri'")
        val bucket = uri.getAuthority
        val prefix = uri.getPath.drop(1)
        val bytes = IOUtils.toByteArray(hook(this, band, s3client.getObject(bucket, prefix).getObjectContent))
        GeoTiffReader.readSingleband(bytes)
      }
    val tiles = tifs.map(_.tile).toArray
    val extent = tifs.head.extent
    val crs = tifs.head.crs
    ProjectedRaster(MultibandTile(tiles), extent, crs)
  }

  def getMtlFromS3(
    s3client: AmazonS3Client = S3Client.default,
    hook: IOHook = IOHook.passthrough
  ): MTL = {
    val uri = new URI(mtlUri)
    val bucket = uri.getAuthority
    val prefix = uri.getPath.drop(1)
    logger.info(s"Getting $sceneId MTL from '$uri'")
    val stream = hook(this, "MTL", s3client.getObject(bucket, prefix).getObjectContent)
    try {
      MTL.fromStream(stream)
    } finally { stream.close() }
  }

  def getFromS3(
    bandsWanted: Seq[String],
    s3client: AmazonS3Client = S3Client.default,
    hook: IOHook = IOHook.passthrough
  ): LandsatRaster = {
    LandsatRaster(
      getMtlFromS3(s3client, hook),
      bandsWanted,
      getRasterFromS3(bandsWanted, s3client, hook)
    )
  }
}
