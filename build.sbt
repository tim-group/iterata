organization := "com.timgroup"
name := "iterata"

homepage := Some(url("https://github.com/tim-group/iterata"))
licenses := Seq("MIT" -> url("http://opensource.org/licenses/MIT"))

scalaVersion := "2.11.8"

// Compilation options
scalacOptions ++= Seq("-unchecked", "-deprecation")

//
// Dependencies
//
libraryDependencies += "org.scalatest" %% "scalatest" % "2.2.5" % "test"

// Don't download javadocs for transitive dependencies,
// see https://github.com/mpeltonen/sbt-idea/issues/225#issuecomment-19150022
transitiveClassifiers in Global := Seq(Artifact.SourceClassifier)

overridePublishSettings
