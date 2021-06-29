import xerial.sbt.Sonatype._
import ReleaseTransformations._

name := "weaver-test-extra"
organization in ThisBuild := "io.github.dimitarg"

scalaVersion in ThisBuild := "2.13.4"
crossScalaVersions in ThisBuild := Seq("2.13.4", "2.12.12")

githubWorkflowScalaVersions in ThisBuild := Seq("2.13.4", "2.12.12")
githubWorkflowBuild in ThisBuild := Seq(WorkflowStep.Sbt(List("coverage", "test", "coverageReport")))
githubWorkflowEnv in ThisBuild += "CODECOV_TOKEN" -> "${{ secrets.CODECOV_TOKEN }}"
githubWorkflowEnv in ThisBuild += "PGP_PASSPHRASE" -> "${{ secrets.PGP_PASSPHRASE }}"
githubWorkflowEnv in ThisBuild += "PGP_SECRET" -> "${{ secrets.PGP_SECRET }}"
githubWorkflowEnv in ThisBuild += "SONATYPE_USERNAME" -> "${{ secrets.SONATYPE_USERNAME }}"
githubWorkflowEnv in ThisBuild += "SONATYPE_PASSWORD" -> "${{ secrets.SONATYPE_PASSWORD }}"


licenses in ThisBuild += ("Apache-2.0", url("https://opensource.org/licenses/Apache-2.0"))

githubWorkflowBuildPostamble in ThisBuild := Seq(WorkflowStep.Run(
  commands = List("bash <(curl -s https://codecov.io/bash)")
))

githubWorkflowPublishPreamble in ThisBuild := Seq(WorkflowStep.Run(
  List(
    "git config user.name \"Github Actions (dimitarg/weaver-test-extra)\"",
    "gpg --keyserver hkps://keyserver.ubuntu.com --recv-keys A5131D4F48321D6E",
    "echo $PGP_SECRET | base64 --decode --ignore-garbage | gpg --batch --passphrase $PGP_PASSPHRASE --import"
  )
))

githubWorkflowPublish in ThisBuild := Seq(WorkflowStep.Sbt(List("release cross with-defaults")))

libraryDependencies +=  "com.disneystreaming" %% "weaver-scalacheck" % "0.5.1"
libraryDependencies +=  "com.disneystreaming" %% "weaver-framework" % "0.5.1"

libraryDependencies += "co.fs2" %% "fs2-io" % "2.5.7" % "test"

testFrameworks += new TestFramework("weaver.framework.TestFramework")

addCompilerPlugin("org.typelevel" % "kind-projector" % "0.11.2" cross CrossVersion.full)

releasePublishArtifactsAction := PgpKeys.publishSigned.value

usePgpKeyHex("B08DBE76A33A8E25468188D5A5131D4F48321D6E")

publishTo  in ThisBuild := sonatypePublishToBundle.value

sonatypeCredentialHost := "s01.oss.sonatype.org"
sonatypeRepository := "https://s01.oss.sonatype.org/service/local"

publishMavenStyle  in ThisBuild := true

sonatypeProjectHosting := Some(GitHubHosting("dimitarg", "weaver-test-extra", "dimitar.georgiev.bg@gmail.com"))

developers := List(
  Developer(id="dimitarg", name="Dimitar Georgiev", email="dimitar.georgiev.bg@gmail.com", url=url("https://dimitarg.github.io/"))
)

releaseCrossBuild := true // true if you cross-build the project for multiple Scala versions
releaseProcess := Seq[ReleaseStep](
  checkSnapshotDependencies,
  inquireVersions,
  runClean,
  runTest,
  setReleaseVersion,
  commitReleaseVersion,
  tagRelease,
  // For non cross-build projects, use releaseStepCommand("publishSigned")
  releaseStepCommandAndRemaining("+publishSigned"),
  releaseStepCommand("sonatypeBundleRelease"),
  setNextVersion,
  commitNextVersion,
  pushChanges
)
