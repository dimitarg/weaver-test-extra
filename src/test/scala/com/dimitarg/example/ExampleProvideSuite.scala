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

  override def suitesStream: fs2.Stream[IO, RTest[Unit]] =
      Stream.resource(res).flatMap { r =>
          all.provideShared(r) ++
          FooSuite.all.using[SharedResource](_.foo).provideShared(r) ++
          BarSuite.all.using[SharedResource](_.bar).provide(res)

      }
}
