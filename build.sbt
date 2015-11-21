name := "scala-landsat-util"
version := "0.1.0"
scalaVersion := "2.10.5"
crossScalaVersions := Seq("2.11.5", "2.10.5")
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

libraryDependencies ++= Seq(
  "io.spray"        %% "spray-json"    % "1.3.2",
  "io.spray"        %% "spray-client"  % "1.3.2",
  "io.spray"        %% "spray-httpx"   % "1.3.2",
  "com.typesafe.akka" %% "akka-actor"   % "2.3.9",
  "com.github.nscala-time" %% "nscala-time" % "1.6.0",
  "org.scalatest"       %%  "scalatest"      % "2.2.0" % "test"
)
