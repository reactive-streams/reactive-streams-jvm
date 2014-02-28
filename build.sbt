organization := "org.asyncrx"

version := "0.1-SNAPSHOT"

licenses := Seq("CC0" -> url("http://creativecommons.org/publicdomain/zero/1.0/"))

homepage := Some(url("https://groups.google.com/forum/?hl=en#!forum/reactive-streams"))

scalaVersion := "2.10.3"

lazy val spi = project

lazy val tck = project.dependsOn(spi)
