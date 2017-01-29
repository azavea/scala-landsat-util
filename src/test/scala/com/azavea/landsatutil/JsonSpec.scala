package com.azavea.landsatutil

import org.scalatest._
import spray.json._

import Json._

class JsonSpec extends FunSpec with Matchers {

  describe("JsonReaders") {
    it("should deserialize an image json") {
      val json = Resource.string("/test-image.json")
      val image = json.parseJson.convertTo[LandsatImage]
      image.thumbnailUrl should be ("http://earthexplorer.usgs.gov/browse/landsat_8/2015/015/032/LC80150322015213LGN00.jpg")
    }

    it("should deserialize metadata json") {
      val json = Resource.string("/test-meta.json")
      val meta = json.parseJson.convertTo[QueryMetadata]
      meta.found should be (173)
    }

    it("should deserialize example json response from api") {
      val json = Resource.string("/test-response.json")
      val result = json.parseJson.convertTo[QueryResult]

      result.images.size should be (173)
    }
  }
}
