package com.dimitarg.example

import java.time.Instant

import cats.effect.IO
import fs2.Stream
import weaver.pure._

object ExampleSuite extends Suite {

  override def suitesStream: Stream[IO, Test] = Stream(
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
