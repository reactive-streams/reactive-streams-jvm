organization in ThisBuild := "org.reactivestreams"

version in ThisBuild := "0.4.0"

scalaVersion in ThisBuild := "2.10.3"

licenses in ThisBuild := Seq("CC0" -> url("http://creativecommons.org/publicdomain/zero/1.0/"))

homepage in ThisBuild := Some(url("http://www.reactive-streams.org/"))

publishTo in ThisBuild := Some("releases" at "https://oss.sonatype.org/service/local/staging/deploy/maven2")

lazy val api = project

lazy val tck = project.dependsOn(api)

lazy val examples = project.dependsOn(api)

publishArtifact := false // for this aggregate project

EclipseKeys.projectFlavor in ThisBuild := EclipseProjectFlavor.Java

pomExtra in ThisBuild := (
  <scm>
    <url>git@github.com:reactive-streams/reactive-streams.git</url>
    <connection>scm:git:git@github.com:reactive-streams/reactive-streams.git</connection>
  </scm>
  <developers>
    <developer>
      <id>reactive-streams-sig</id>
      <name>Reactive Streams SIG</name>
      <url>http://www.reactive-streams.org/</url>
    </developer>
  </developers>
  )
