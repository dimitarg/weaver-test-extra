package com.dimitarg.example.anyjunk.legacy

import weaver._
import cats.effect.IO

object Vanilla extends SimpleIOSuite {

  // note return type of `test`, or lack thereof
  test("a test") {
    for {
      _ <- IO("hi")
    } yield expect(1 == 1)
  }

  test("another") {
    IO.unit.as(expect(2 == 2))
  }
}
