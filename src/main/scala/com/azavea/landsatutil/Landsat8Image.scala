package com.azavea.landsatutil

import org.joda.time.DateTime

case class QueryResult(metadata: QueryMetadata, images: Seq[LandsatImage])

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
  lowerLeft: (Double, Double),
  lowerRight: (Double, Double),
  upperLeft: (Double, Double),
  upperRight: (Double, Double),
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
  private def basePath = f"$path%03d/$row%03d/$sceneId"
  def rootUri = s"s3://landsat-pds/L8/$basePath"
  def bandUri(band: Int) = s"s3://landsat-pds/L8/$basePath/${sceneId}_B${band}.TIF"
  def largeThumbnail = s"https://landsat-pds.s3.amazonaws.com/L8/$basePath/${sceneId}_thumb_large.jpg"
  def smallThumbnail = s"https://landsat-pds.s3.amazonaws.com/L8/$basePath/${sceneId}_thumb_small.jpg"

  def footprint: String = {
    val ll = f"${lowerLeft._1}%0,5f,${lowerLeft._2}%0,5f"
    val lr = f"${lowerRight._1}%0,5f,${lowerRight._2}%0,5f"
    val ul = f"${upperLeft._1}%0,5f,${upperLeft._2}%0,5f"
    val ur = f"${upperRight._1}%0,5f,${upperRight._2}%0,5f"
    s"""{"type":"Polygon","coordinates":[[[$ll],[$lr],[$ur],[$ul],[$ll]]]}"""
  }
}
