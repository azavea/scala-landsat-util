import scala.util.Properties

object Version {
  val landsatUtil = {
    val tag = Properties.envOrElse("TRAVIS_TAG", "")
    if(tag == "") {
      "0.1.0" + Properties.envOrElse("LSU_VERSION_SUFFIX", "-SNAPSHOT")
    } else {
      tag
    }
  }

  val geotrellis  = "0.10.0-9d08114"
  val scala       = "2.10.5"
  lazy val hadoop      = "2.2.0"
  lazy val spark       = "1.4.1"
}
