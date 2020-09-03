# weaver-test-extra

Provides extra functionality to https://github.com/disneystreaming/weaver-test

Currently, the following functionality is provided

## package `weaver.pure`

Provides minimal, ready-to-use API to `weaver-test`, that does not perform side effects
in order to compute the tests in a test suite (i.e. "register" a test).


# Why this library exists

`weaver-test` is a very good testing library, fundamentally because tests in
`weaver-test` are values.

However, default ready-to-use APIs in `weaver-test` assume that one can "register a test" with the framework, which is a side effect i.e. takes the form

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
over and over.

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