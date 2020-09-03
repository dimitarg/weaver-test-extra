package com.dimitarg.example

import cats.effect.IO
import fs2.Stream
import weaver.pure._

object BarSuite {
  val tests: Stream[IO, RTest[BarResource]] = Stream(
    rTest("a barsuite test") { r =>
      expect(r.value == 42)
    }
  )
}
