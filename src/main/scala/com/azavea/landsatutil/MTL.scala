package com.azavea.landsatutil

import shapeless._
import shapeless.syntax.typeable._

import java.io._

case class MtlGroup(name: String, fields: Map[String, Any]) {
  def apply[T: Typeable](fieldName: String): Option[T] = {
    // seems weird, but safely casting primitive and references is actually tricky
    fields.get(fieldName).flatMap(_.cast[T])
  }

  override def toString = s"MtlGroup($name)"
}

case class MTL(group: Map[String, MtlGroup]) {
  def metadataFileInfo: MtlGroup = group("METADATA_FILE_INFO")
  def productMetadata: MtlGroup = group("PRODUCT_METADATA")
  def imageAttributes: MtlGroup = group("IMAGE_ATTRIBUTES")
  def minMaxRadiance: MtlGroup = group("MIN_MAX_RADIANCE")
  def minMaxReflectance: MtlGroup = group("MIN_MAX_REFLECTANCE")
  def minMaxPixelValue: MtlGroup = group("MIN_MAX_PIXEL_VALUE")
  def radiometricRescaling: MtlGroup = group("RADIOMETRIC_RESCALING")
  def tirsThermalConstants: MtlGroup = group("TIRS_THERMAL_CONSTANTS")
  def projectionParameters: MtlGroup = group("PROJECTION_PARAMETERS")
}

object MTL {
  def fromFile(file: String): MTL =
    MtlParser(new FileReader(file)).get

  def fromStream(stream: InputStream): MTL =
    MtlParser(new BufferedReader(new InputStreamReader(stream))).get

  def fromString(str: String): MTL =
    MtlParser(new StringReader(str)).get
}
