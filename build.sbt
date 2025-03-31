import xerial.sbt.Sonatype._
import ReleaseTransformations._

name := "weaver-test-extra"
ThisBuild / organization := "io.github.dimitarg"

ThisBuild / scalaVersion := "2.13.15"
ThisBuild / crossScalaVersions := Seq("2.13.15", "2.12.20", "3.3.4")
ThisBuild / githubWorkflowScalaVersions := Seq("2.13.15", "2.12.20", "3.3.4")

ThisBuild / githubWorkflowJavaVersions := Seq(JavaSpec.temurin("21"))

ThisBuild / githubWorkflowBuild := Seq(
  WorkflowStep.Sbt(
    commands = List("scalafmtCheck")
  ),
  WorkflowStep.Sbt(
    // scoverage plugin not yet supporting scala 2.13.15
    // commands = List("coverage", "test"),
    commands = List("test"),
    env = Map(
      "HONEYCOMB_WRITE_KEY" -> "${{ secrets.HONEYCOMB_WRITE_KEY }}"
    )
  )
  // scoverage plugin not yet supporting scala 2.13.15
  // WorkflowStep.Sbt(List("coverageReport")),
)
ThisBuild / githubWorkflowEnv += "CODECOV_TOKEN" -> "${{ secrets.CODECOV_TOKEN }}"
ThisBuild / githubWorkflowEnv += "PGP_PASSPHRASE" -> "${{ secrets.PGP_PASSPHRASE }}"
ThisBuild / githubWorkflowEnv += "PGP_SECRET" -> "${{ secrets.PGP_SECRET }}"
ThisBuild / githubWorkflowEnv += "SONATYPE_USERNAME" -> "${{ secrets.SONATYPE_USERNAME }}"
ThisBuild / githubWorkflowEnv += "SONATYPE_PASSWORD" -> "${{ secrets.SONATYPE_PASSWORD }}"
ThisBuild / githubWorkflowTargetBranches := Seq("master")
ThisBuild / githubWorkflowPublishTargetBranches += RefPredicate.Equals(Ref.Branch("master"))

ThisBuild / licenses += ("Apache-2.0", url("https://opensource.org/licenses/Apache-2.0"))

// scoverage plugin not yet supporting scala 2.13.15
// ThisBuild / githubWorkflowBuildPostamble := Seq(WorkflowStep.Run(
//   commands = List("bash <(curl -s https://codecov.io/bash)")
// ))

ThisBuild / githubWorkflowPublishPreamble := Seq(
  WorkflowStep.Run(
    List(
      "git config user.name \"Github Actions (dimitarg/weaver-test-extra)\"",
      "git config user.email \"dimitar.georgiev.bg@gmail.com\"",
      "gpg --keyserver hkps://keyserver.ubuntu.com --recv-keys 7A723A868B1FD65C8108ACAF00437AAD7A33298A",
      "echo $PGP_SECRET | base64 --decode --ignore-garbage | gpg --batch --passphrase $PGP_PASSPHRASE --import"
    )
  )
)

ThisBuild / githubWorkflowPublish := Seq(WorkflowStep.Sbt(List("release cross with-defaults")))

val weaverVersion = "0.8.4"

val natchezVersion = "0.3.7"
val fs2Version = "3.12.0"

libraryDependencies ++= Seq(
  "com.disneystreaming" %% "weaver-scalacheck" % weaverVersion,
  "com.disneystreaming" %% "weaver-cats" % weaverVersion,
  "org.tpolecat" %% "natchez-core" % natchezVersion,
  "org.tpolecat" %% "natchez-noop" % natchezVersion % "test",
  "org.tpolecat" %% "natchez-honeycomb" % natchezVersion % "test",
  "co.fs2" %% "fs2-io" % fs2Version % "test"
)

testFrameworks += new TestFramework("weaver.framework.CatsEffect")

libraryDependencies ++= {
  CrossVersion.partialVersion(scalaVersion.value) match {
    case Some((2, n)) =>
      List(
        compilerPlugin("org.typelevel" % "kind-projector" % "0.13.3" cross CrossVersion.full)
      )
    case _ =>
      Nil
  }
}

ThisBuild / scalacOptions ++= {
  CrossVersion.partialVersion(scalaVersion.value) match {
    case Some((2, 12 | 13)) => Seq("-Xsource:3-cross", "-P:kind-projector:underscore-placeholders")
    case _                  => Nil
  }
}

releasePublishArtifactsAction := PgpKeys.publishSigned.value

usePgpKeyHex("7A723A868B1FD65C8108ACAF00437AAD7A33298A")

ThisBuild / publishTo := sonatypePublishToBundle.value

sonatypeCredentialHost := sonatypeCentralHost

ThisBuild / publishMavenStyle := true

sonatypeProjectHosting := Some(GitHubHosting("dimitarg", "weaver-test-extra", "dimitar.georgiev.bg@gmail.com"))

developers := List(
  Developer(
    id = "dimitarg",
    name = "Dimitar Georgiev",
    email = "dimitar.georgiev.bg@gmail.com",
    url = url("https://dimitarg.github.io/")
  )
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
