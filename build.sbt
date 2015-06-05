// Don't download javadocs for transitive dependencies, see https://github.com/mpeltonen/sbt-idea/issues/225#issuecomment-19150022
transitiveClassifiers in Global := Seq(Artifact.SourceClassifier)

libraryDependencies += "org.scalatest" %% "scalatest" % "2.2.5" % "test"

scalaVersion := "2.11.6"

// moves this stuff to ./project/publish.sbt

publishTo := {
  val nexus = "https://oss.sonatype.org/"
  if (isSnapshot.value)
    Some("snapshots" at nexus + "content/repositories/snapshots")
  else
    Some("releases"  at nexus + "service/local/staging/deploy/maven2")
}

// Create this file to publish to Sonatype as TIM Group. File Contents:
//   realm=Sonatype Nexus Repository Manager
//   host=oss.sonatype.org
//   user=<USERNAME>
//   password=<PASSWORD>
//
credentials += Credentials(Path.userHome / ".timgroup_sonatype_credentials")

// Make sure that you have sbt-pgp configured, and that you have generated a signing key
//   http://www.scala-sbt.org/sbt-pgp/
