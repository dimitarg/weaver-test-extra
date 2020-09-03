package com.dimitarg.example

import weaver.pure._
import cats.effect.IO

object ExampleSuite extends Suite {

  override def suitesStream: fs2.Stream[IO,RTest[Unit]] = tests(
      test("a pure test") {
          val x = 1
          expect(x == 1)
      },
      test("another pure test") {
        val xs = List()
        expect(xs == List())
      }
  )
}
