////
// To release a new version of this project, type
//
//      sbt release
//
// Then, you must release the deployment from oss.sonatype.org to Maven Central,
// see http://central.sonatype.org/pages/releasing-the-deployment.html
//
// In order to publish, you will first need to have your sbt-pgp key configured
// and published, and your TIM Group Sonatype credentials file present, please
// see below for more details.
////

//
// Force sbt-pgp to ignore the system gpg and use default jvm implementation
// and file locations, see http://www.scala-sbt.org/sbt-pgp/usage.html
//
useGpg := false
pgpSecretRing := Path.userHome / ".sbt" / "gpg" / "secring.asc"
pgpPublicRing := Path.userHome / ".sbt" / "gpg" / "pubring.asc"

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

pomExtra := (
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
)

releasePublishArtifactsAction := PgpKeys.publishSigned.value
