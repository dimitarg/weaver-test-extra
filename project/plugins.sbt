addSbtPlugin("org.scalameta" % "sbt-scalafmt" % "2.6.2")

// handles gh actions and release process for us
addSbtPlugin("org.typelevel" % "sbt-typelevel" % "0.8.7")

addSbtPlugin("org.scala-js" % "sbt-scalajs" % "1.22.0")

// dependency update reports (`dependencyUpdates`)
addSbtPlugin("com.timushev.sbt" % "sbt-updates" % "0.7.1")
