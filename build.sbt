organization in ThisBuild := "org.reactivestreams"

version in ThisBuild := "0.3"

scalaVersion in ThisBuild := "2.10.3"

licenses in ThisBuild := Seq("CC0" -> url("http://creativecommons.org/publicdomain/zero/1.0/"))

homepage in ThisBuild := Some(url("https://groups.google.com/forum/?hl=en#!forum/reactive-streams"))

publishTo in ThisBuild := Some("releases" at "https://oss.sonatype.org/service/local/staging/deploy/maven2")

lazy val spi = project

lazy val tck = project.dependsOn(spi)

publishArtifact := false // for this aggregate project
