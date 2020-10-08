name := "weaver-test-extra"
organization in ThisBuild := "io.github.dimitarg"

scalaVersion in ThisBuild := "2.13.3"
crossScalaVersions in ThisBuild := Seq("2.13.3")

githubWorkflowScalaVersions in ThisBuild := Seq("2.13.3")
githubWorkflowBuild in ThisBuild := Seq(WorkflowStep.Sbt(List("coverage", "test", "coverageReport")))
githubWorkflowEnv in ThisBuild += "CODECOV_TOKEN" -> "${{ secrets.CODECOV_TOKEN }}"
githubWorkflowEnv in ThisBuild += "BINTRAY_USER" -> "${{ secrets.BINTRAY_USER }}"
githubWorkflowEnv in ThisBuild += "BINTRAY_PASS" -> "${{ secrets.BINTRAY_PASS }}"

licenses in ThisBuild += ("Apache-2.0", url("https://opensource.org/licenses/Apache-2.0"))

githubWorkflowBuildPostamble in ThisBuild := Seq(WorkflowStep.Run(
  commands = List("bash <(curl -s https://codecov.io/bash)")
))

githubWorkflowPublishPreamble in ThisBuild := Seq(WorkflowStep.Run(
  List("git config user.name \"Github Actions (dimitarg/weaver-test-extra)\"")
))
githubWorkflowPublish in ThisBuild := Seq(WorkflowStep.Sbt(List("release with-defaults")))

libraryDependencies +=  "com.disneystreaming" %% "weaver-scalacheck" % "0.5.0"
libraryDependencies +=  "com.disneystreaming" %% "weaver-framework" % "0.5.0"

libraryDependencies += "co.fs2" %% "fs2-io" % "2.4.4" % "test"

testFrameworks += new TestFramework("weaver.framework.TestFramework")

addCompilerPlugin("org.typelevel" %% "kind-projector" % "0.11.0" cross CrossVersion.full)

