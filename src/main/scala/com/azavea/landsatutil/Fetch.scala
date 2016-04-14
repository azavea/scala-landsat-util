package com.azavea.landsatutil

import geotrellis.raster._
import geotrellis.raster.io.geotiff.reader._
import com.amazonaws.services.s3._
import org.apache.commons.io._
import org.apache.commons.compress.compressors.bzip2._
import org.apache.commons.compress.archivers._
import org.apache.commons.compress.archivers.tar._
import java.io._
import java.net.{ URL, URI }

object Fetch {
  def fromTar(url: URL, bandsWanted: Seq[String]): (MTL, ProjectedRaster[MultibandTile]) = {
    val stream = url.openStream
    try {
      fromTar(stream, bandsWanted)
    } finally { stream.close }
  }

  def fromTar(file: File, bandsWanted: Seq[String]): (MTL, ProjectedRaster[MultibandTile]) = {
    val stream = new FileInputStream(file)
    try {
      fromTar(stream, bandsWanted)
    } finally { stream.close }
  }

  def fromTar(stream: InputStream, bandsWanted: Seq[String]): (MTL, ProjectedRaster[MultibandTile]) = {
    val buffStream = new BufferedInputStream(stream)
    val bzStream = new BZip2CompressorInputStream(buffStream, true)
    val tarStream = new TarArchiveInputStream(bzStream)

    val rxBandName = """LC.+_(.+)\..+""".r
    var mtl: MTL = null
    var rs = Map.empty[String, ProjectedRaster[Tile]]
    var entry: TarArchiveEntry = tarStream.getNextTarEntry

    while (entry != null) {
      val rxBandName(bandName) = entry.getName
      if (bandName == "MTL") {
        val buf = new Array[Byte](entry.getSize.toInt)
        tarStream.read(buf)
        mtl = MTL.fromString(new String(buf))
      } else if (bandsWanted.contains(bandName.drop(1))) {
        val buf = new Array[Byte](entry.getSize.toInt)
        tarStream.read(buf)
        val tif = GeoTiffReader.readSingleband(buf)
        rs = rs updated (bandName, ProjectedRaster(tif.tile, tif.extent, tif.crs))
      }
      entry = tarStream.getNextTarEntry
    }

    val tiles = for (bandName <- bandsWanted) yield rs(s"B$bandName").tile
    val extent = rs.head._2.extent
    val crs = rs.head._2.crs
    (mtl, ProjectedRaster(MultibandTile(tiles), extent, crs))
  }

  def fromS3(image: LandsatImage, bandsWanted: Seq[String], awsClient: AmazonS3Client): ProjectedRaster[MultibandTile] = {
    val tifs =
      for (band <- bandsWanted) yield {
        val uri = new URI(image.bandUri(band))
        val bucket = uri.getAuthority
        val prefix = uri.getPath.drop(1)
        val bytes = IOUtils.toByteArray(awsClient.getObject(bucket, prefix).getObjectContent)
        GeoTiffReader.readSingleband(bytes)
      }
    val tiles = tifs.map(_.tile).toArray
    val extent = tifs.head.extent
    val crs = tifs.head.crs
    ProjectedRaster(MultibandTile(tiles), extent, crs)
  }

  def mtl(image: LandsatImage, awsClient: AmazonS3Client): MTL = {
    val uri = new URI(image.mtlUri)
    val bucket = uri.getAuthority
    val prefix = uri.getPath.drop(1)
    val stream = awsClient.getObject(bucket, prefix).getObjectContent
    try {
      MTL.fromStream(stream)
    } finally { stream.close }
  }
}
