package com.dimitarg.example.anyjunk.test

import weaver.pure._
import fs2._
import cats.effect.IO
import cats.effect.Resource

object BeforeAfter extends Suite {

   val beforeAfter = Resource.make[IO, Unit] {
     IO(println("acquiring")).as(())
   } { _ =>
     IO(println("releasing"))
   }

  override def suitesStream: Stream[IO,Test] = 
    Stream.resource(beforeAfter).flatMap { _ =>
      Stream(
        test("beforeAfter is just Stream.flatMap") {
          IO.unit.as(expect(1 == 1))
        },
        test("another test") {
          IO.unit.as(expect(2 == 2))
        }
      )
    }
  
}
