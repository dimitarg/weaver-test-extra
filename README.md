 [ ![Download](https://api.bintray.com/packages/dimitarg/maven/weaver-test-extra/images/download.svg) ](https://bintray.com/dimitarg/maven/weaver-test-extra/_latestVersion)
[![Code Coverage](https://codecov.io/gh/dimitarg/weaver-test-extra/branch/master/graph/badge.svg?token=CJ7FXTIAPX)](https://codecov.io/gh/dimitarg/weaver-test-extra)
 ![Build status](https://github.com/dimitarg/weaver-test-extra/workflows/Continuous%20Integration/badge.svg?branch=master)

# weaver-test-extra

> "Constraints liberate, liberties constrain." - Rúnar Bjarnason

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
tests to be ran, our test suite becomes a referentially transparent expression, and we can reap the benefits of that.

One very practical benefit of that is principled resource sharing and suite setup / teardown, which in a referentially transparent world are simply achieved via `Resource` / `Stream` and passing parameters to functions.

See also [this article](https://dimitarg.github.io/pure-testing-scala/) for a more verbose treatment of why purely functional testing is a good idea. 
*(The above article is outdated as it uses an old vesion of this library which had an unnecessarily compplicated API, but the principles still apply.)*


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
    testFrameworks += new TestFramework("weaver.framework.CatsEffect")
```

# Base concepts

, of which there is just one.

## Test

A test is defined as follows:

```scala
final case class Test(name: String, run: IO[Expectations])
```

That is to say, a test has a name, returns а test result
`Expectations`, and is allowed to perform `IO` while doing so.


## Creating an `Test`

... can be done by constructing an instance of the above data type, either by calling its data constructor or 
via the function `weaver.pure.test`.

# Usage example

## Hello world

Here is a simple suite. You create one by extending `weaver.pure.Suite`, which requires returning a `fs2.Stream` of tests:

```scala
package com.dimitarg.example

import java.time.Instant

import cats.effect.IO
import fs2.Stream
import weaver.pure._

object ExampleSuite extends Suite {

  override def suitesStream: fs2.Stream[IO, Test] = Stream(
      pureTest("a pure test") {
          val x = 1
          expect(x == 1)
      },
      pureTest("another pure test") {
        val xs = List()
        expect(xs == List())
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
that the function `test` returns a value of type `Test`, instead of performing a side effect to register the test in some internal mutable state.

A note on types:
- use `pureTest` when your test body has type `Expectations`, i.e. no effects are performed by the test
- use `test` when your test body has type `IO[Expectations]` 
- `pureTest("foo"){ expect(1 == 1) }` is just syntax sugar for `test("foo"){ expect(1 == 1).pure[IO] }`

## Suite of tests that share a common resource

I.e. like `beforeAll`, but not hideous.

Since a suite (or sub-suite) of tests has type `Stream[IO, Test]`, and [sharing a resource is just passing a parameter](https://youtu.be/bCcEHRkFfbY?t=1946), a suite that uses some suite-wide "resource" of type `R` has type `R => Stream[IO, Test]`.

### Example

```scala
package com.dimitarg.example

import fs2.Stream
import weaver.pure._
import cats.effect.{IO, Resource}
import scala.concurrent.ExecutionContext
import java.util.concurrent.Executors

object ExampleResSuite extends Suite {

  // shared resource
  final case class TextFile(lines: List[String])

  // describe how to acquire shared resource
  val sharedResource: Resource[IO, List[String]] = for {
    ec <- Resource.make(
        IO(ExecutionContext.fromExecutorService(Executors.newCachedThreadPool()))
    )( x =>
        IO(x.shutdown)
    )
    xs = fs2.io.readInputStream(
        IO(getClass().getResourceAsStream("/foo.txt")),
        1024, closeAfterUse = true
    ) 
    lines <- Resource.eval(
        xs.through(fs2.text.utf8.decode).through(fs2.text.lines).compile.toList
    )
  } yield lines


  // suite which uses shared resource
  val suites: List[String] => Stream[IO, Test] = lines => Stream(
    pureTest("the file has one line") {
      expect(lines.size == 1)
    },
    pureTest("the file has the expected content") {
      expect(lines == List("Hello, there!"))    
    }
  )

  // construct `suitesStream` by acquiring resource and passing that to your `suite` via `flatMap`
  override def suitesStream: fs2.Stream[IO, Test] =
    Stream.resource(sharedResource).flatMap { res =>
      suites(res)
    }
  
}
```

No magic!

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
- Construct a value `dbTests: Transactor[IO] => Stream[IO, Test]` for the database tests
- Construct a value `httpTests: Client[IO] => Stream[IO, Test]` for the http tests
- Construct a value `e2eTests: TestResources => Stream[IO, Test]` for the end to end tests
- Combine the resulting streams into a single stream with type `Stream[IO, Test]`,
by `flatMap`-ping over the shared resource, and providing the necessary resources to individual suites:


### Example

```scala
package com.dimitarg.example.sharedres

import fs2.Stream
import cats.effect.{IO, Resource}
import weaver.pure._


final case class FooResource()
final case class BarResource(value: Int)
final case class SharedResource(foo: FooResource, bar: BarResource)

object FooSuite {

  val all: FooResource => Stream[IO, Test] = foo => Stream(
    pureTest("the foo foos") {
        expect(foo == FooResource())
    }
  )
}

object BarSuite {
  val all: BarResource => Stream[IO, Test] = bar => Stream(
    pureTest("a barsuite test") {
      expect(bar.value == 42)
    }
  )
}

object ExampleSharedResSuite extends Suite {

  val mkSharedResource: Resource[IO, SharedResource] = for {
    _ <- Resource.eval(IO.pure(println("acquiring shared resource")))
    res <- Resource.eval(IO.pure(
      SharedResource(FooResource(), BarResource(42))
    ))
  } yield res

  val suiteUsingAllResources: SharedResource => Stream[IO, Test] = res => Stream(
    pureTest("some test"){
      expect(res.bar.value == 42)
  })

  override def suitesStream: fs2.Stream[IO, Test] =
    Stream.resource(mkSharedResource).flatMap { r =>
      suiteUsingAllResources(r) ++
      FooSuite.all(r.foo) ++
      BarSuite.all(r.bar)
    }
}
```

Notes:
- `FooSuite` and `BarSuite` do not need to extend anything from `weaver-test` or `weaver-test-extra`, they are just containers of `A => Stream[IO, Test]` values. This of course also means they would not be auto-discoverable or runnable on their own. A small price to pay for 
principled, resource-safe test setup and teardown.
