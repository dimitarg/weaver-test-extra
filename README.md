# weaver-test-extra

Provides extra functionality to https://github.com/disneystreaming/weaver-test

Currently, the following functionality is provided

- `weaver.pure` - Provides ready-to-use pure interface for registering tests, as opposed
to registering tests via a side effect. In addition, allows to reuse a Resource across multiple test suites
in a referentially transparent manner.

# Why this library exists

`weaver-test` is an excellent library which makes the observation that the **definition** of a test is just a value:

(roughly, minus explicit cruft)

```scala
object Test {

  def apply[F[_]](name: String, f: Log[F] => F[Expectations]): F[TestOutcome] = ...
}
```

That is to say, a test has a name, can potentially access a `weaver-test` log,
returns `Expectations`, and does so in some effect type `F`.

However, the default API provided for **registering** tests is side-effectful:

```scala
def registerTest(name: String)(f: Res => F[TestOutcome]): Unit = ...
```

`weaver-test-extra` goes an extra mile by providing a small API which doesn't talk about registering tests;
instead, the user is in charge of returning the tests to be ran in a suite. Therefore, your  whole test codebase becomes 
free of side effects.

One consequence of this is that sharing a suite-wide resource between multiple test modules (files) means
just passing function parameters - as per https://youtu.be/aeGQiq4HTTA?t=1814


`weaver-test-extra` is an addition to `weaver-test` that I find useful when writing tests. It can be used alongside the
standard APIs exposed by `weaver-test`.


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
final case class RTest[R](name: String, run: (Log[IO], R) => IO[Expectations])
```

That is to say, a test has a name, and is a function which has access to a Log and
some "environment" `R` (`R` stands for `Resource`, more on that later), returns
`Expectations` and is allowed to perform `IO` while doing so.

Tests that do not require an environment have a type `RTest[Unit]`.

## Creating an `RTest`

... can be done by constructing an instance of the above data type.
There are convenience functions `test`, `rTest`, `loggedTest` and `loggedRTest` for doing so
in the module `weaver.test.pure`.

We will see example usage of each of these below.

# Usage example

## Hello world

Here is a simple suite. You create one by extending `weaver.pure.Suite`, which requires returning a `fs2.Stream` of tests:

```
import weaver.pure._
import cats.effect.IO
import java.time.Instant

object ExampleSuite extends Suite {

  override def suitesStream: fs2.Stream[IO,RTest[Unit]] = tests(
      test("a pure test") {
          val x = 1
          expect(x == 1)
      },
      test("another pure test") {
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
that the function `test` returns a value of `RTest`, instead of performing a side effect to register the test in some internal mutable state.

The outer function `tests` just constructs an `fs2.Stream` from a bunch of elements (in this case tests). It's just an alias for
`fs2.Stream.apply`.

Couple of notes

- The type of `suitesStream` is `fs2.Stream[IO,RTest[Unit]]` because we require no 
environment / shared test resource. If that was not the case, we'd extend `ResourceSuite` instead
(see below)

The two pure tests above typecheck because there is an implicit conversion in scope
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
3. By implementing `def sharedResource: Resource[IO, R]`, you specify how to acquire (and release)
the shared resource.
4. You create a test that requres access to some resource / environment by using `rTest` instead of `test`.
5. The shared resource will be acquired once before running all the tests, and will be released
when the tests are ran (regardless of their outcome).