package com.azavea.landsatutil

import java.time.LocalDate

import org.scalatest._

class MtlSpec extends FunSpec with Matchers {
  val m: MTL = MTL.fromStream(Resource.stream("/MTL.txt"))

  it("should produce None given wrong type"){
    m.metadataFileInfo.fields.contains("STATION_ID") should be (true)
    m.metadataFileInfo[Int]("STATION_ID") should be (None)
  }

  it("should be able to access String field"){
    m.metadataFileInfo[String]("STATION_ID") should be (Some("LGN"))
  }

  it("should be able to access Int field"){
    info(m.group("PRODUCT_METADATA").fields("WRS_PATH").getClass.toString)
    m.productMetadata[Int]("WRS_PATH") should be (Some(14))
  }

  it("should be able to access Double field"){
    info(m.group("IMAGE_ATTRIBUTES").fields("CLOUD_COVER").getClass.toString)
    m.imageAttributes[Double]("CLOUD_COVER") should be (Some(65.99))
  }

  it("should be able to access LocalDate field"){
    info(m.group("PRODUCT_METADATA").fields("DATE_ACQUIRED").getClass.toString)
    m.productMetadata[LocalDate]("DATE_ACQUIRED") should be (Some(LocalDate.of(2015,8,10)))
  }
}
