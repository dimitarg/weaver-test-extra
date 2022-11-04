package com.dimitarg.example.util

import cats.effect.IO

sealed trait IntegrationTestConfig

object IntegrationTestConfig {

  final case class CI(
    honeycombWriteKey: String,
  )  extends IntegrationTestConfig {
    override def toString = "IntegrationTestConfig.CI()"
  }

  final case object NotCI extends IntegrationTestConfig

  def load: IO[IntegrationTestConfig] = for {
    isCi <- IO.delay {
      Option(System.getenv("CI")).isDefined
    }
    hcKey <- IO.delay {
      Option(System.getenv("HONEYCOMB_WRITE_KEY"))
    }
  } yield hcKey.fold[IntegrationTestConfig] {
    IntegrationTestConfig.NotCI
  } { hcKey => 
    if (isCi) {
      IntegrationTestConfig.CI(hcKey)
    } else {
      IntegrationTestConfig.NotCI
    }
  }
}
