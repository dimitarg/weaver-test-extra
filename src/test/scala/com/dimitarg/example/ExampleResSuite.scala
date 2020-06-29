package com.dimitarg.example

import fs2.Stream
import cats.effect.{IO, Resource}
import weaver.extra.res._

object ExampleResSuite extends PureResourceSuite {
  override type R = SharedResource

  override def sharedResource: Resource[IO, SharedResource] = for {
    _ <- Resource.liftF(IO.pure(println("acquiring shared resource")))
    res <- Resource.liftF(IO.pure(
      SharedResource(FooResource(), BarResource(42))
    ))
  } yield res

  override def suitesStream: fs2.Stream[IO, RTest[SharedResource]] =
      Stream(
        test[SharedResource]("some test")(res => {
        expect(res.bar.value == 42)
      })) ++
      Stream.emits(FooSuite.tests)
        .local[SharedResource](_.foo) ++
      BarSuite.tests
        .local(_.bar)
}
