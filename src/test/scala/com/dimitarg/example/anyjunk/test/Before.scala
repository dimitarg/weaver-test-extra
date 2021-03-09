package com.dimitarg.example.anyjunk.test

import weaver.pure._
import fs2._
import cats.effect.IO
import cats.effect.Resource

object Before extends Suite {

  override def suitesStream: Stream[IO,Test] = Stream(
  
    test("before is just Resource.use") {
      val beforeAfter = Resource.make[IO, Unit] {
        IO(println("acquiring")).as(())
      } { _ =>
        IO(println("releasing"))
      }

      beforeAfter.use { _ =>
        IO.unit.as(expect(1 == 1))
      }
    }
    
  )
  
}
