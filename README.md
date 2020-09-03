# weaver-test-extra

Provides extra functionality to https://github.com/disneystreaming/weaver-test

Currently, the following functionality is provided

- `weaver.extra.pure` - Provides ready-to use pure interface for registering tests, as opposed
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
returns `Expectations`, and does some in some effect type `F`.

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

, of which there are very few.

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

... can be done by just constructing an instance of the above data type.
There are convenience functions `test`, `rTest`, `loggedTest` and `loggedRTest` for doing so
in the module `weaver.test.pure`.

# Usage example

## Pure test

The simplest example we start with is a test that needs to perform no effects

```
import weaver.pure._
import cats.effect.IO

object ExampleSuite extends Suite {

  override def suitesStream: fs2.Stream[IO,RTest[Unit]] = tests(
      test("a pure test") {
          val x = 1
          expect(x == 1)
      },
      test("another pure test") {
        val xs = List()
        expect(xs == List())
      }
  )
}
```

This is pretty much 1:1 with what you'd write in `weaver-test` vanilla; with the important distinction
that the function `test` returns a value of `RTest`, instead of performing a side effect.

Here we know in advance that this suite has just two tests, but in general a suite 
(in `weaver-test-extra`, as well as vanilla `weaver-test`) is a `fs2.Stream` of test descriptions.

Therefore we call the convenience function `tests` to turn the (in this case two) `RTest`s into
a `fs2.Stream[IO, RTest[Unit]]`.
