name := "scala-landsat-util"
version := Version.landsatUtil
scalaVersion := Version.scala
description := "API client for Developmentseed's landsat-api"
organization := "com.azavea"
licenses := Seq("Apache-2.0" -> url("http://www.apache.org/licenses/LICENSE-2.0.html"))
scalacOptions ++= Seq(
  "-deprecation",
  "-unchecked",
  "-Yinline-warnings",
  "-language:implicitConversions",
  "-language:reflectiveCalls",
  "-language:higherKinds",
  "-language:postfixOps",
  "-language:existentials",
  "-feature")
publishMavenStyle := true
publishArtifact in Test := false
pomIncludeRepository := { _ => false }

shellPrompt := { s => Project.extract(s).currentProject.id + " > " }

resolvers += "LocationTech GeoTrellis Releases" at "https://repo.locationtech.org/content/repositories/geotrellis-releases"

libraryDependencies ++= Seq(
  "io.spray" %% "spray-json" % "1.3.2",
  "io.spray" %% "spray-client" % "1.3.3",
  "io.spray" %% "spray-httpx" % "1.3.3",
  "com.typesafe.akka" %% "akka-actor" % "2.3.16",
  "org.locationtech.geotrellis" %% "geotrellis-vector" % Version.geotrellis % "provided",
  "org.locationtech.geotrellis" %% "geotrellis-raster" % Version.geotrellis % "provided",
  "org.apache.commons" % "commons-compress" % "1.12",
  "org.apache.commons" % "commons-io" % "1.3.2",
  "com.typesafe.scala-logging" %% "scala-logging" % "3.5.0",
  "com.amazonaws" % "aws-java-sdk-s3" % "1.11.60",
  "com.chuusai" %% "shapeless" % "2.3.2",
  "org.scala-lang.modules" %% "scala-parser-combinators" % "1.0.4",
  "org.scalatest" %% "scalatest" % "3.0.1" % "test"
)

bintrayOrganization := Some("azavea")
bintrayRepository := "maven"
bintrayVcsUrl := Some("https://github.com/azavea/scala-landsat-util.git")
bintrayPackageLabels := Seq("scala", "landsat", "maps", "gis", "geographic", "data", "raster", "processing")

initialCommands in console :=
  """
import com.azavea.landsatutil._
import geotrellis.vector._
import geotrellis.vector.io._
import geotrellis.vector.io.json.GeoJson
import geotrellis.raster._
  """
