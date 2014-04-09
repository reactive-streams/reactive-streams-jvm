name := "reactive-streams-spi"

javacOptions in compile ++= Seq("-encoding", "UTF-8", "-source", "1.6", "-target", "1.6", "-Xlint:unchecked", "-Xlint:deprecation")

autoScalaLibrary := false

crossPaths := false

publishMavenStyle := true

Common.javadocSettings
