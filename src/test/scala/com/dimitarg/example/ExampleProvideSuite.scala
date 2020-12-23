package com.dimitarg.example.sharedres

import fs2.Stream
import cats.effect.{IO, Resource}
import weaver.pure._

object ExampleProvideSuite extends Suite {

  val res: Resource[IO, SharedResource] = for {
    _ <- Resource.liftF(IO.pure(println("acquiring shared resource")))
    res <- Resource.liftF(IO.pure(
      SharedResource(FooResource(), BarResource(42))
    ))
  } yield res

  val all: Stream[IO, RTest[SharedResource]] = Stream(
    rTest[SharedResource]("some test")(res => {
      expect(res.bar.value == 42)
  }))

  override def suitesStream: fs2.Stream[IO, Test] =
      Stream.resource(res).flatMap { r =>
          all.provide(r) ++
          FooSuite.all.local[SharedResource](_.foo).provide(r) ++
          BarSuite.all.local[SharedResource](_.bar).provideResource(res)

      }
}
