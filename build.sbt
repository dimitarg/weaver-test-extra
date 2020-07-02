name := "weaver-test-playground"

version := "0.1"

scalaVersion in ThisBuild := "2.13.3"
crossScalaVersions in ThisBuild := Seq("2.13.3")
githubWorkflowScalaVersions in ThisBuild := Seq("2.13.3")
githubWorkflowBuild in ThisBuild := Seq(WorkflowStep.Sbt(List("coverage", "test", "coverageReport")))

libraryDependencies +=  "com.disneystreaming" %% "weaver-scalacheck" % "0.4.2-RC1"
libraryDependencies +=  "com.disneystreaming" %% "weaver-framework" % "0.4.2-RC1"

testFrameworks += new TestFramework("weaver.framework.TestFramework")

addCompilerPlugin("org.typelevel" %% "kind-projector" % "0.11.0" cross CrossVersion.full)

