package com.azavea.landsatutil

import org.joda.time.DateTime
import geotrellis.vector._

case class QueryResult(metadata: QueryMetadata, images: Seq[LandsatImage]) {
  def mapImages(f: Seq[LandsatImage] => Seq[LandsatImage]): QueryResult =
    QueryResult(metadata: QueryMetadata, f(images))
}

case class QueryMetadata(
  total: Int,
  skip: Int,
  limit: Int,
  lastUpdated: DateTime
)

case class LandsatImage(
  sceneId: String,
  satellite: String,
  row: Int,
  path: Int,
  aquisitionDate: DateTime,
  cloudPercentage: Double,
  thumbnailUrl: String,
  lowerLeft: Point,
  lowerRight: Point,
  upperLeft: Point,
  upperRight: Point,
  sceneStartTime: DateTime,
  sceneEndTime: DateTime,
  imageQuality: Int,
  sunAzmith: Double,
  sunElevation: Double,
  dayTime: Boolean,
  sensor: String,
  receivingStation: String,
  dateUpdated: DateTime
) {
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
}
