name := "reactive-streams-spi"

javacOptions in compile ++= Seq("-encoding", "UTF-8", "-source", "1.6", "-target", "1.6", "-Xlint:unchecked", "-Xlint:deprecation")

javacOptions in (Compile,doc) ++= Seq("-encoding","UTF-8","-docencoding", "UTF-8", "-charset", "UTF-8", "-notimestamp", "-linksource")

autoScalaLibrary := false

crossPaths := false

publishMavenStyle := true

Common.javadocSettings
