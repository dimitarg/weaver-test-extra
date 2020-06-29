package com.dimitarg.example

import cats.effect.IO
import fs2.Stream
import cats.implicits._
import weaver.extra.res._

object BarSuite extends Tests {
  val tests: Stream[IO, RTest[BarResource]] = Stream(
    test("a barsuite test") { r =>
      expect(r.value == 42)
    }
  )

}
