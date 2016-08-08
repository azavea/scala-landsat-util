import scala.util.Properties

object Version {
  val landsatUtil = {
    val tag = Properties.envOrElse("TRAVIS_TAG", "")
    if(tag == "") {
      "0.2.1" + Properties.envOrElse("LSU_VERSION_SUFFIX", "-SNAPSHOT")
    } else {
      tag
    }
  }

  val geotrellis  = "0.10.0"
  val scala       = "2.10.6"
}
