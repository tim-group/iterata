organization := "com.timgroup"
name := "iterata"

homepage := Some(url("https://github.com/tim-group/iterata"))
licenses := Seq("MIT" -> url("http://opensource.org/licenses/MIT"))

// Don't download javadocs for transitive dependencies, see https://github.com/mpeltonen/sbt-idea/issues/225#issuecomment-19150022
transitiveClassifiers in Global := Seq(Artifact.SourceClassifier)

libraryDependencies += "org.scalatest" %% "scalatest" % "2.2.5" % "test"

scalaVersion := "2.11.6"

////
// TODO: Move this stuff to ./project/publish.sbt
//
// To release a new version of this project, type
//
//      sbt release
//
// Then, you must release the deployment from oss.sonatype.org to Maven Central,
// see http://central.sonatype.org/pages/releasing-the-deployment.html
//
// In order to publish, you will need to have your sbt-pgp key configured and
// published, and your TIM Group sonatype credentials file present, please see
// below for details.
////

//
// Below are the standard configuration steps to publish to Maven Central via
// oss.sonatype.org using sbt. For more information, please see
//   http://www.scala-sbt.org/0.13/docs/Using-Sonatype.html
//

publishTo := {
  val nexus = "https://oss.sonatype.org/"
  if (isSnapshot.value)
    Some("snapshots" at nexus + "content/repositories/snapshots")
  else
    Some("releases"  at nexus + "service/local/staging/deploy/maven2")
}

// In order publish to oss.sonatype.org as TIM Group, you must create
// the file `~/.timgroup_sonatype_credentials` with the following lines:
//
//     realm=Sonatype Nexus Repository Manager
//     host=oss.sonatype.org
//     user=<USERNAME>
//     password=<PASSWORD>
//
credentials += Credentials(Path.userHome / ".timgroup_sonatype_credentials")

publishMavenStyle := true

publishArtifact in Test := false

pomIncludeRepository := { _ => false }

pomExtra :=
  <url>https://github.com/tim-group/iterata</url>
  <licenses>
    <license>
      <name>MIT</name>
      <url>http://opensource.org/licenses/MIT</url>
      <distribution>repo</distribution>
    </license>
  </licenses>
  <scm>
    <url>git@github.com:tim-group/iterata.git</url>
    <connection>scm:git:git@github.com:tim-group/iterata.git</connection>
  </scm>
  <developers>
    <developer>
      <id>ms-tg</id>
      <name>Marc Siegel</name>
      <url>https://github.com/ms-tg</url>
    </developer>
  </developers>
