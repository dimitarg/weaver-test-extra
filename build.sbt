name := "weaver-test-extra"
organization in ThisBuild := "io.github.dimitarg"

version := "0.1"

scalaVersion in ThisBuild := "2.13.3"
crossScalaVersions in ThisBuild := Seq("2.13.3")

githubWorkflowScalaVersions in ThisBuild := Seq("2.13.3")
githubWorkflowBuild in ThisBuild := Seq(WorkflowStep.Sbt(List("coverage", "test", "coverageReport")))
githubWorkflowEnv in ThisBuild += "CODECOV_TOKEN" -> "${{ secrets.CODECOV_TOKEN }}"
githubWorkflowBuildPostamble in ThisBuild := Seq(WorkflowStep.Run(
  commands = List("bash <(curl -s https://codecov.io/bash)")
))

githubWorkflowPublishPreamble in ThisBuild := Seq(WorkflowStep.Run(
  List("git config user.name \"Github Actions (dimitarg/weaver-test-extra)\"")
))
githubWorkflowPublish in ThisBuild := Seq(WorkflowStep.Sbt(List("release with-defaults")))

githubOwner in ThisBuild := "dimitarg"
githubRepository in ThisBuild := "weaver-test-extra"

libraryDependencies +=  "com.disneystreaming" %% "weaver-scalacheck" % "0.4.2-RC1"
libraryDependencies +=  "com.disneystreaming" %% "weaver-framework" % "0.4.2-RC1"

testFrameworks += new TestFramework("weaver.framework.TestFramework")

addCompilerPlugin("org.typelevel" %% "kind-projector" % "0.11.0" cross CrossVersion.full)

