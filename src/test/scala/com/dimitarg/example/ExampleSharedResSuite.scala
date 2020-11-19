package com.dimitarg.example.sharedres

import fs2.Stream
import cats.effect.{IO, Resource}
import weaver.pure._


final case class FooResource()
final case class BarResource(value: Int)
final case class SharedResource(foo: FooResource, bar: BarResource)

object FooSuite {

  val all: Stream[IO, RTest[FooResource]] = Stream(
    rTest("the foo foos") { foo =>
        expect(foo == FooResource())
    }
  )
}

object BarSuite {
  val all: Stream[IO, RTest[BarResource]] = Stream(
    rTest("a barsuite test") { r =>
      expect(r.value == 43)
    }
  )
}

object ExampleSharedResSuite extends RSuite {
  override type R = SharedResource

  override def sharedResource: Resource[IO, SharedResource] = for {
    _ <- Resource.liftF(IO.pure(println("acquiring shared resource")))
    res <- Resource.liftF(IO.pure(
      SharedResource(FooResource(), BarResource(42))
    ))
  } yield res

  val all: Stream[IO, RTest[SharedResource]] = Stream(
    rTest[SharedResource]("some test")(res => {
      expect(res.bar.value == 42)
  }))

  override def suitesStream: fs2.Stream[IO, RTest[SharedResource]] =
      all ++
      FooSuite.all
        .using[SharedResource](_.foo) ++
      BarSuite.all
        .using[SharedResource](_.bar)
}
