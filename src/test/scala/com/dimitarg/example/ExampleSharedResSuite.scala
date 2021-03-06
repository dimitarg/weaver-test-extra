package com.dimitarg.example.sharedres

import fs2.Stream
import cats.effect.{IO, Resource}
import weaver.pure._


final case class FooResource()
final case class BarResource(value: Int)
final case class SharedResource(foo: FooResource, bar: BarResource)

object FooSuite {

  val all: FooResource => Stream[IO, Test] = foo => Stream(
    test("the foo foos") {
        expect(foo == FooResource())
    }
  )
}

object BarSuite {
  val all: BarResource => Stream[IO, Test] = bar => Stream(
    test("a barsuite test") {
      expect(bar.value == 42)
    }
  )
}

object ExampleSharedResSuite extends Suite {

  val mkSharedResource: Resource[IO, SharedResource] = for {
    _ <- Resource.liftF(IO.pure(println("acquiring shared resource")))
    res <- Resource.liftF(IO.pure(
      SharedResource(FooResource(), BarResource(42))
    ))
  } yield res

  val suiteUsingAllResources: SharedResource => Stream[IO, Test] = res => Stream(
    test("some test"){
      expect(res.bar.value == 42)
  })

  override def suitesStream: fs2.Stream[IO, Test] =
    Stream.resource(mkSharedResource).flatMap { r =>
      suiteUsingAllResources(r) ++
      FooSuite.all(r.foo) ++
      BarSuite.all(r.bar)
    }
}
