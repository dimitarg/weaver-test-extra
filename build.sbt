import sbtcrossproject.CrossProject
ThisBuild / tlBaseVersion := "0.8" // Our current series x.y

name := "weaver-test-extra"

ThisBuild / organization := "io.github.dimitarg"

ThisBuild / startYear := Some(2020)
ThisBuild / licenses := Seq(License.Apache2)
ThisBuild / developers := List(
  // your GitHub handle and name
  tlGitHubDev("dimitarg", "Dimitar Georgiev")
)

ThisBuild / scalaVersion := "2.13.18"
ThisBuild / crossScalaVersions := Seq("2.13.18", "3.9.0")

ThisBuild / githubWorkflowEnv += "HONEYCOMB_WRITE_KEY" -> "${{ secrets.HONEYCOMB_WRITE_KEY }}"

ThisBuild / githubWorkflowTargetBranches := Seq("master")
ThisBuild / githubWorkflowPublishTargetBranches += RefPredicate.Equals(Ref.Branch("master"))

ThisBuild / githubWorkflowJavaVersions := Seq(JavaSpec.temurin("21"))

ThisBuild / tlCiHeaderCheck := false

val weaverVersion = "0.13.0"
val fs2Version = "3.14.0"
val otel4sVersion = "1.1.0"
val openTelemetryVersion = "1.67.0"

lazy val commonSettings = Seq(
  testFrameworks += new TestFramework("weaver.framework.CatsEffect"),
  libraryDependencies ++= {
    CrossVersion.partialVersion(scalaVersion.value) match {
      case Some((2, n)) =>
        List(
          compilerPlugin("org.typelevel" % "kind-projector" % "0.13.4" cross CrossVersion.full)
        )
      case _ =>
        Nil
    }
  }
)

lazy val core = CrossProject("weaver-test-extra", file("modules/core"))(JSPlatform, JVMPlatform)
  .withoutSuffixFor(JVMPlatform)
  .settings(commonSettings)
  .settings(
    libraryDependencies ++= Seq(
      "org.typelevel" %%% "weaver-scalacheck" % weaverVersion,
      "org.typelevel" %%% "weaver-cats" % weaverVersion,
      "co.fs2" %%% "fs2-io" % fs2Version % "test"
    )
  )
  .jsSettings(
    libraryDependencies ++= Seq(
      // 2.8.0+ is built with Scala 3.9 and breaks our Scala 3.3 LTS build
      "io.github.cquiroz" %%% "scala-java-time" % "2.7.0"
    )
  )

// in principle this can be made cross-platform e.g. by using the otel scala sdk, but out of scope for now.
lazy val traced = (project in file("modules/traced"))
  .dependsOn(core.jvm)
  .settings(commonSettings)
  .settings(
    name := "weaver-test-extra-otel4s",
    libraryDependencies ++= Seq(
      "org.typelevel" %% "otel4s-core" % otel4sVersion,

      "org.typelevel" %% "otel4s-oteljava" % otel4sVersion,
      "io.opentelemetry" % "opentelemetry-exporter-otlp" % openTelemetryVersion % Test,
      "io.opentelemetry" % "opentelemetry-sdk-extension-autoconfigure" % openTelemetryVersion % Test
    )
  )

lazy val root = tlCrossRootProject.aggregate(core.jvm, core.js, traced)
