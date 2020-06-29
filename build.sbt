name := "weaver-test-playground"

version := "0.1"

scalaVersion := "2.13.3"
libraryDependencies +=  "com.disneystreaming" %% "weaver-scalacheck" % "0.4.2-RC1" % Test
libraryDependencies +=  "com.disneystreaming" %% "weaver-framework" % "0.4.2-RC1" % Test

testFrameworks += new TestFramework("weaver.framework.TestFramework")
