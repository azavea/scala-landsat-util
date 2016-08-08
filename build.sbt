name := "scala-landsat-util"
version := Version.landsatUtil
scalaVersion := "2.10.6"
crossScalaVersions := Seq("2.11.8", "2.10.6")
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

resolvers += Resolver.bintrayRepo("azavea", "geotrellis")

libraryDependencies ++= Seq(
  "io.spray"        %% "spray-json"    % "1.3.2",
  "io.spray"        %% "spray-client"  % "1.3.2",
  "io.spray"        %% "spray-httpx"   % "1.3.2",
  "com.typesafe.akka" %% "akka-actor"   % "2.3.9",
  "com.github.nscala-time" %% "nscala-time" % "2.12.0",
  "com.azavea.geotrellis" %% "geotrellis-vector" % Version.geotrellis,
  "com.azavea.geotrellis" %% "geotrellis-raster" % Version.geotrellis,
  "org.apache.commons" % "commons-compress" % "1.8",
  "org.apache.commons" % "commons-io" % "1.3.2",
  "com.typesafe.scala-logging" %% "scala-logging-slf4j" % "2.1.2",
  "com.amazonaws" % "aws-java-sdk-s3" % "1.9.34",
  "com.chuusai" %% "shapeless" % "2.3.0",
  "org.scalatest"       %%  "scalatest"      % "2.2.0" % "test"
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
import com.github.nscala_time.time.Imports._
"""
