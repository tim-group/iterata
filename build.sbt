// Don't download javadocs for transitive dependencies, see https://github.com/mpeltonen/sbt-idea/issues/225#issuecomment-19150022
transitiveClassifiers in Global := Seq(Artifact.SourceClassifier)

libraryDependencies += "org.scalatest" %% "scalatest" % "2.2.5" % "test"

scalaVersion := "2.11.6"

// moves this stuff to ./project/publish.sbt
publishTo := Some(Resolver.file("file",  new File(Path.userHome.absolutePath + "/temp_sbt")))

//credentials += Credentials(Path.userHome / ".timgroup_sonatype_credentials")
