import scala.util.Properties

object Version {
  val landsatUtil = {
    val tag = Properties.envOrElse("TRAVIS_TAG", "")
    if(tag == "") {
      "1.0.0" + Properties.envOrElse("LSU_VERSION_SUFFIX", "-SNAPSHOT")
    } else {
      tag
    }
  }

  val geotrellis  =  "1.0.0-RC1"
  val akka        = "2.4.14"
  val akkaHttp    = "10.0.0"
  val scala       = "2.11.8"
}
