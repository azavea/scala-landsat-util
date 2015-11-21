# Landsat API client for Scala

This is a scala client for Developmentseed's [landsat-api](https://github.com/developmentseed/landsat-api/)

### Example usages

```scala
  val images =
    Landsat8Query()
      .withStartDate(new DateTime(2015, 1, 12, 0, 0, 0))
      .withEndDate(new DateTime(2015, 11, 5, 0, 0, 0))
      .contains(-75.26596069335938,39.88296828403436,-75.05859375,40.01351528489102)
      .collect()

  images.filter(_.cloudPercentage < 10).take(5).map { image =>
    println(s"${image.sceneId} - ${image.cloudPercentage}% clouds")
    println(image.thumbnailUrl)
    println(image.largeThumbnail)
    println(image.smallThumbnail)
    println(image.footprint)
  }

  println(s"Results: ${images.size} images.")
```

```scala
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
```
