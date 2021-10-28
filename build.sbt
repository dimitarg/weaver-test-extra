import xerial.sbt.Sonatype._
import ReleaseTransformations._

name := "weaver-test-extra"
ThisBuild / organization := "io.github.dimitarg"

ThisBuild / scalaVersion := "2.13.6"
ThisBuild / crossScalaVersions := Seq("2.13.6", "2.12.15")

ThisBuild / githubWorkflowScalaVersions  := Seq("2.13.6", "2.12.15")
ThisBuild / githubWorkflowBuild := Seq(WorkflowStep.Sbt(List("coverage", "test", "coverageReport")))
ThisBuild / githubWorkflowEnv += "CODECOV_TOKEN" -> "${{ secrets.CODECOV_TOKEN }}"
ThisBuild / githubWorkflowEnv += "PGP_PASSPHRASE" -> "${{ secrets.PGP_PASSPHRASE }}"
ThisBuild / githubWorkflowEnv += "PGP_SECRET" -> "${{ secrets.PGP_SECRET }}"
ThisBuild / githubWorkflowEnv += "SONATYPE_USERNAME" -> "${{ secrets.SONATYPE_USERNAME }}"
ThisBuild / githubWorkflowEnv += "SONATYPE_PASSWORD" -> "${{ secrets.SONATYPE_PASSWORD }}"
ThisBuild / githubWorkflowPublishTargetBranches += RefPredicate.Equals(Ref.Branch("master"))


ThisBuild / licenses += ("Apache-2.0", url("https://opensource.org/licenses/Apache-2.0"))

ThisBuild / githubWorkflowBuildPostamble := Seq(WorkflowStep.Run(
  commands = List("bash <(curl -s https://codecov.io/bash)")
))

ThisBuild / githubWorkflowPublishPreamble := Seq(WorkflowStep.Run(
  List(
    "git config user.name \"Github Actions (dimitarg/weaver-test-extra)\"",
    "gpg --keyserver hkps://keyserver.ubuntu.com --recv-keys A5131D4F48321D6E",
    "echo $PGP_SECRET | base64 --decode --ignore-garbage | gpg --batch --passphrase $PGP_PASSPHRASE --import"
  )
))

ThisBuild / githubWorkflowPublish := Seq(WorkflowStep.Sbt(List("release cross with-defaults")))

libraryDependencies +=  "com.disneystreaming" %% "weaver-scalacheck" % "0.7.7"
libraryDependencies +=  "com.disneystreaming" %% "weaver-cats" % "0.7.7"

libraryDependencies += "co.fs2" %% "fs2-io" % "3.2.2" % "test"

testFrameworks += new TestFramework("weaver.framework.CatsEffect")

addCompilerPlugin("org.typelevel" % "kind-projector" % "0.13.2" cross CrossVersion.full)

releasePublishArtifactsAction := PgpKeys.publishSigned.value

usePgpKeyHex("B08DBE76A33A8E25468188D5A5131D4F48321D6E")

ThisBuild / publishTo := sonatypePublishToBundle.value

sonatypeCredentialHost := "s01.oss.sonatype.org"
sonatypeRepository := "https://s01.oss.sonatype.org/service/local"

ThisBuild / publishMavenStyle := true

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
