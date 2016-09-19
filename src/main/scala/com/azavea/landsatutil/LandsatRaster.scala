package com.azavea.landsatutil

import geotrellis.raster._
import geotrellis.raster.io.geotiff.reader._
import org.apache.commons.compress.compressors.bzip2._
import org.apache.commons.compress.archivers.tar._
import java.io._

case class LandsatRaster(
  mtl: MTL,
  bands: Seq[String],
  raster: ProjectedRaster[MultibandTile]
) {
  def apply(bandName: String): ProjectedRaster[Tile] = {
    val bandIndex = bands.indexOf(bandName)
    require(bandIndex > -1, "$bandName not found")
    ProjectedRaster(raster.tile.bands(bandIndex), raster.extent, raster.crs)
  }

  def apply(bandIndex: Int): ProjectedRaster[Tile] = {
    require(bandIndex > -1 && bandIndex < bands.length, "$bandIndex out of bounds")
    ProjectedRaster(raster.tile.bands(bandIndex), raster.extent, raster.crs)
  }
}

object LandsatRaster {
  /** Extract Landsat bands from tar.gz input stream */
  def fromTarBz(stream: InputStream, bandsWanted: Seq[String]): LandsatRaster = {
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
    LandsatRaster(mtl, bandsWanted, ProjectedRaster(MultibandTile(tiles), extent, crs))
  }

  /** Extract Landsat bands from tar.gz file */
  def fromTarBz(file: File, bandsWanted: Seq[String]): LandsatRaster = {
    val stream = new FileInputStream(file)
    try {
      fromTarBz(stream, bandsWanted)
    } finally { stream.close() }
  }
}
