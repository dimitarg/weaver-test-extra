package com.dimitarg.example.anyjunk.test

import weaver.pure._
import fs2._
import cats.effect.IO

object TableDriven extends Suite {

  final case class Example(x: Int, y: Int)

  override def suitesStream: Stream[IO,Test] = {
    
    val examples = List(
      Example( 1,    1   ),
      Example(-3,    11  ),
      Example( 6,    42  )
    )

    Stream.emits(examples).map { case Example(x, y) =>
      test(s"sum is commutative - $x + $y") {
        expect(x + y == y + x)
      }
    }
  }
  
}
