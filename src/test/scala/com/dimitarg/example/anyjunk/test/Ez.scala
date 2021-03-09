package com.dimitarg.example.anyjunk.test

import weaver.pure._
import fs2._
import cats.effect.IO

object Ez extends Suite {

  override def suitesStream: Stream[IO,Test] = Stream( // <- note return type
  
    test("test") {
      for {
        _ <- IO(println("yo"))
      } yield expect(1 == 1)
    },

    test("another test") {
      IO.unit.as(expect(true))
    }
  )
  
}
