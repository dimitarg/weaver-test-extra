ThisBuild / tlBaseVersion := "0.6" // Our current series x.y

name := "weaver-test-extra"

ThisBuild / organization := "io.github.dimitarg"

ThisBuild / startYear := Some(2020)
ThisBuild / licenses := Seq(License.Apache2)
ThisBuild / developers := List(
  // your GitHub handle and name
  tlGitHubDev("dimitarg", "Dimitar Georgiev")
)

ThisBuild / scalaVersion := "2.13.16"
ThisBuild / crossScalaVersions := Seq("2.13.16", "3.3.6")


ThisBuild / githubWorkflowEnv += "CODECOV_TOKEN" -> "${{ secrets.CODECOV_TOKEN }}"
ThisBuild / githubWorkflowEnv += "HONEYCOMB_WRITE_KEY" -> "${{ secrets.HONEYCOMB_WRITE_KEY }}"

ThisBuild / githubWorkflowTargetBranches := Seq("master")
ThisBuild / githubWorkflowPublishTargetBranches += RefPredicate.Equals(Ref.Branch("master"))

ThisBuild / githubWorkflowJavaVersions := Seq(JavaSpec.temurin("21"))


ThisBuild / tlCiHeaderCheck := false
val weaverVersion = "0.9.0"

val natchezVersion = "0.3.8"
val fs2Version = "3.12.0"

lazy val core = crossProject(JSPlatform, JVMPlatform)
  .withoutSuffixFor(JVMPlatform)
  .in(file("modules/core"))
  .settings(
    libraryDependencies ++= Seq(
      "org.typelevel" %%% "weaver-scalacheck" % weaverVersion,
      "org.typelevel" %%% "weaver-cats" % weaverVersion,
      "org.tpolecat" %%% "natchez-core" % natchezVersion,
      "org.tpolecat" %%% "natchez-noop" % natchezVersion % "test",
      "org.tpolecat" %% "natchez-honeycomb" % natchezVersion % "test",
      "co.fs2" %%% "fs2-io" % fs2Version % "test"
    ),
    testFrameworks += new TestFramework("weaver.framework.CatsEffect"),
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
  )
  // .settings(publishAndReleaseSettings)
  .jsSettings(
    libraryDependencies ++= Seq(
      "io.github.cquiroz" %%% "scala-java-time" % "2.6.0"
    )
  )

lazy val root = tlCrossRootProject.aggregate(core.jvm, core.js)

// TODO delete all this commented stuff once we've got release working again

// ThisBuild / githubWorkflowPublishPreamble := Seq(
//   WorkflowStep.Run(
//     List(
//       "git config user.name \"Github Actions (dimitarg/weaver-test-extra)\"",
//       "git config user.email \"dimitar.georgiev.bg@gmail.com\"",
//       "gpg --keyserver hkps://keyserver.ubuntu.com --recv-keys 7A723A868B1FD65C8108ACAF00437AAD7A33298A",
//       "echo $PGP_SECRET | base64 --decode --ignore-garbage | gpg --batch --passphrase $PGP_PASSPHRASE --import"
//     )
//   )
// )

// ThisBuild / githubWorkflowPublish := Seq(WorkflowStep.Sbt(List("release cross with-defaults")))

// ThisBuild / sonatypeCredentialHost := sonatypeCentralHost
// ThisBuild / sonatypeProjectHosting := Some(GitHubHosting("dimitarg", "weaver-test-extra", "dimitar.georgiev.bg@gmail.com"))
// ThisBuild / developers := List(
//       Developer(
//         id = "dimitarg",
//         name = "Dimitar Georgiev",
//         email = "dimitar.georgiev.bg@gmail.com",
//         url = url("https://dimitarg.github.io/")
//       )
//     )

// ThisBuild / releaseCrossBuild := true // true if you cross-build the project for multiple Scala versions
// ThisBuild /releaseProcess := Seq[ReleaseStep](
//       checkSnapshotDependencies,
//       inquireVersions,
//       runClean,
//       runTest,
//       setReleaseVersion,
//       commitReleaseVersion,
//       tagRelease,
//       // For non cross-build projects, use releaseStepCommand("publishSigned")
//       releaseStepCommandAndRemaining("+publishSigned"),
//       releaseStepCommand("sonatypeBundleRelease"),
//       setNextVersion,
//       commitNextVersion,
//       pushChanges
// )

// val publishAndReleaseSettings = Seq(
//   publishTo := sonatypePublishToBundle.value,
//   publishMavenStyle := true,
//   usePgpKeyHex("7A723A868B1FD65C8108ACAF00437AAD7A33298A"),
//   releasePublishArtifactsAction := PgpKeys.publishSigned.value
// )
