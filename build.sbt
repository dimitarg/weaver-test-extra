import xerial.sbt.Sonatype._
import ReleaseTransformations._

name := "weaver-test-extra"
ThisBuild / organization := "io.github.dimitarg"

ThisBuild / scalaVersion := "2.13.8"
ThisBuild / crossScalaVersions := Seq("2.13.8", "2.12.15")

ThisBuild / githubWorkflowScalaVersions  := Seq("2.13.8", "2.12.15")
ThisBuild / githubWorkflowBuild := Seq(
  WorkflowStep.Sbt(
    commands = List("coverage", "test"),
    env = Map(
      "HONEYCOMB_WRITE_KEY" -> "${{ secrets.HONEYCOMB_WRITE_KEY }}",
    )
  ),
  WorkflowStep.Sbt(List("coverageReport")),
)
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

val weaverVersion = "0.7.15"
val natchezVersion = "0.2.1"

libraryDependencies ++=Seq(
  "com.disneystreaming" %% "weaver-scalacheck" % weaverVersion,
  "com.disneystreaming" %% "weaver-cats" % weaverVersion,
  "org.tpolecat" %% "natchez-core" % natchezVersion,

  "org.tpolecat" %% "natchez-noop" % natchezVersion % "test",
  "org.tpolecat" %% "natchez-honeycomb" % natchezVersion % "test",
  "co.fs2" %% "fs2-io" % "3.2.14" % "test",
)  


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
