# weaver-test-extra

Provides extra functionality to https://github.com/disneystreaming/weaver-test

Currently, the following functionality is provided

## package `weaver.pure`

Provides minimal, ready-to-use API to `weaver-test`, that does not perform side effects
in order to compute the tests in a test suite (i.e. "register" a test).


# Why this library exists

Registration of a test to be ran by vanilla `weaver-test` rouglhy takes the form:

```scala
def register(test: Test): Unit
```

If we instead assume that it's always the suite which assembles and returns the `fs2.Stream` of
tests to be ran, we can reap the benefits of all our (test) code being referentially transparent.

In practice this is achieved in just a few lines of code. This library contains little code
aside from documentation.

# Getting started 

1. Add the following resolver to your project settings

```
resolvers += Resolver.bintrayRepo("dimitarg", "maven")
```

2. Add the following dependency to your build

```
"io.github.dimitarg"  %%  "weaver-test-extra"     % <latestVersion> % "test"
```

3. (Not specific to this library, this is a WeaverTest requirement). If you haven't already,
add the following to your project settings

```
    testFrameworks += new TestFramework("weaver.framework.TestFramework")
```

# Base concepts

, of which there is just one.

## RTest

A test is defined as follows:

```scala
final case class RTest[R](name: String, run: R => IO[Expectations])
```

That is to say, a test has a name, and is a function which has access to
some input parameter (environment, shared resource, etc.) `R`, returns
`Expectations` as the test result, and is allowed to perform `IO` while doing so.

Tests that do not require an environment have the type `RTest[Unit]`.

## Creating an `RTest`

... can be done by constructing an instance of the above data type.
There are convenience shorthand functions `test` and `rTest` which construct a test that does not,
and does require an environment.

# Usage example

## Hello world

Here is a simple suite. You create one by extending `weaver.pure.Suite`, which requires returning a `fs2.Stream` of tests:

```
import fs2.Stream
import weaver.pure._
import cats.effect.IO
import java.time.Instant

object ExampleSuite extends Suite {

  override def suitesStream: fs2.Stream[IO,RTest[Unit]] = Stream(
      test("a pure test") {
          val x = 1
          expect(x == 1)
      },
      test("an effectful test") {
        for {
          now <- IO(Instant.now())
          _ <- IO(println(s"current time: $now"))
        } yield expect(1 == 1)
      }
  )
}
```

This is pretty much 1:1 with what you'd write in `weaver-test` vanilla; with the important distinction
that the function `test` returns a value of `RTest`, instead of performing a side effect to register the test in some internal mutable state.

Couple of notes:

- The type of `suitesStream` is `fs2.Stream[IO,RTest[Unit]]` because we require no 
environment / shared test resource. If that was not the case, we'd extend `ResourceSuite` instead
(see below)

- `"a pure test"` typechecks because there is an implicit conversion in scope
`Expectations => IO[Expectations]`. This is done so you don't have to write
```scala
expect(foo == bar).pure[IO]
```
over and over. (This is the same behaviour as vanilla `weaver-test`)

## Suite of tests that share a common resource

I.e. like `beforeAll`, but not hideous.

```

import weaver.pure._
import cats.effect.{IO, Resource}
import scala.concurrent.ExecutionContext
import java.util.concurrent.Executors
import cats.effect.Blocker

object ExampleResSuite extends ResourceSuite {

  override type R = List[String]

  override def sharedResource: Resource[IO, List[String]] = for {
    ec <- Resource.make(
        IO(ExecutionContext.fromExecutorService(Executors.newCachedThreadPool()))
    )( x =>
        IO(x.shutdown)
    )
    blocker = Blocker.liftExecutionContext(ec)
    xs = fs2.io.readInputStream(
        IO(getClass().getResourceAsStream("/foo.txt")),
        1024, blocker, closeAfterUse = true
    ) 
    lines <- Resource.liftF(
        xs.through(fs2.text.utf8Decode).through(fs2.text.lines).compile.toList
    )
  } yield lines

  override def suitesStream: fs2.Stream[IO, RTest[List[String]]] = tests(
      rTest("the file has one line") { lines =>
        expect(lines.size == 1)
      },
      rTest("the file has the expected content") { lines =>
        expect(lines == List("Hello, there!"))    
      }
  )
  
}
```

Let's break it down.

1. To share a resource across all tests in a Suite, you extend `ResourceSuite`.
2. ResourceSuite is polymorphic on the resource type. In this case, `override type R = List[String]`, the list of lines in a file.
3. By implementing `def sharedResource: Resource[IO, R]`, you describe the suite-shared resource, which will be allocated once, used by all the tests, and disposed of, as in `cats.effect.Resource.use`.
4. You create a test that requres access to some resource / environment (i.e. input parameter) by using `rTest` instead of `test`.


No magic.

## Using subsets of a shared resource across multiple modules

Imagine the following use case

- You have a module of database tests requiring, say `transactor: Transactor[IO]`
- You have a module of http integration tests requiring, say `client: Client[IO]`
- You have a module of end-to-end tests, requiring an access to a multitude of resources,
say
```scala
final case class TestResources(transactor: Transactor[IO], client: Client[IO], config: Config, ....)
```

Furthermore, you want to initialise the resources common to multiple test modules (in this example `Transactor[IO]` and `Client[IO]`) only once.

A way to achieve this in this example is to
- Construct a value `dbTests: Stream[IO, RTest[Transactor[IO]]]` for the database tests
- Construct a value `httpTests: Stream[IO, RTest[Client[IO]]]` for the http tests
- Construct a value `e2eTests: Stream[IO, RTest[TestResources]]` for the end to end tests
- Combine the resulting streams into a single stream with type `Stream[IO, RTest[TestResources]]`,
using the `using` function

### `using`

The function is defined as follows

```scala
implicit class RTestStreamOps[R, A](private val xs: Stream[IO, A]) {
    def using[R1](f: R1 => R)(implicit ev: A =:= RTest[R]): Stream[IO, RTest[R1]] =
      xs.map(x => Contravariant[RTest].contramap(x)(f))
  }
```

Minus the implicit cruft this becomes

```scala
  def using[ParentRes](f: ParentRes => ChildRes)(xs: Stream[IO, RTest[ChildRes]]): Stream[IO, RTest[ParentRes]] =
      xs.map(x => Contravariant[RTest].contramap(x)(f))
  }
```

That is to say, we can go `Stream[IO, RTest[ChildRes]] => Stream[IO, RTest[ParentRes]]`, as long as we know how to go `ParentRes => ChildRes`. 

This works because `RTest` is `Contravariant` in `R`

### Example

```scala
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
      expect(r.value == 42)
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
```

Notes:
- `FooSuite` and `BarSuite` do not need to extend anything from `weaver-test` or `weaver-test-extra`, they are just containers of `Stream[IO, RTest[A]]` values. This of course also means they would not
be auto-discoverable or runnable on their own.
