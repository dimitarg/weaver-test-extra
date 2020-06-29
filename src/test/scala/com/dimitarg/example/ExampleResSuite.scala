package com.dimitarg.example

import fs2.Stream
import cats.effect.{IO, Resource}
import weaver.extra.PureResourceSuite

object ExampleResSuite extends PureResourceSuite {
  override type Res = SharedResource

  override def sharedResource: Resource[IO, SharedResource] = for {
   _ <- Resource.liftF(IO.pure(println("acquiring shared resource")))
    res <- Resource.liftF(IO.pure(
      SharedResource(FooResource(), BarResource(42))
    ))
  } yield res

  override def suitesStream: fs2.Stream[IO, PureResourceSuite.Test[SharedResource]] = Stream(
    test("some test")((_, res) => {
      expect(res.bar.value == 42)
    })
  )
}
