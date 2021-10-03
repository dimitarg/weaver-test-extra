package com.dimitarg.example

import fs2.Stream
import weaver.pure._
import cats.effect.IO
import java.time.Instant

object ExampleSuite extends Suite {

  override def suitesStream: fs2.Stream[IO, Test] = Stream(
      pureTest("a pure test") {
          val x = 1
          expect(x == 1)
      },
      pureTest("another pure test") {
        val xs = List()
        expect(xs == List())
      },
      test("an effectful test") {
        for {
          now <- IO(Instant.now())
          _ <- IO(println(s"current time: $now"))
        } yield expect(1 == 1)
      }
  )
}
