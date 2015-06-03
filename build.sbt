// Don't download javadocs for transitive dependencies, see https://github.com/mpeltonen/sbt-idea/issues/225#issuecomment-19150022
transitiveClassifiers in Global := Seq(Artifact.SourceClassifier)

libraryDependencies += "org.scalatest" %% "scalatest" % "2.2.5" % "test"

scalaVersion := "2.11.6"
