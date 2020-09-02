# weaver-test-extra

Provides extra functionality to https://github.com/disneystreaming/weaver-test

Currently, the following functionality is provided

- `weaver.extra.res` - Ability to share `cats.effect.Resource`, across one or multiple test suites, in a referentially transparent manner.

# Getting started 

1. Add the following resolver to your project settings

```
resolvers += Resolver.bintrayRepo("dimitarg", "maven")
```

2. Add the following dependency to your build

```
"io.github.dimitarg"  %%  "weaver-test-extra"     % "0.1" % "test"
```

3. (Not specific to this library, this is a WeaverTest requirement). If you haven't already,
add the following to your project settings

```
    testFrameworks += new TestFramework("weaver.framework.TestFramework")
```

# Usage example

## The problem

The problem we are trying to solve is to acquire a shared resource only once, (e.g. due to performance considerations), and reuse that resource in multiple test suites.

We want to achieve this under the following constraints:

- All the resulting code must be pure. That is, we will not sacrifice referential transparency or local reasoning in order to pass resources between tests;
- We should be able to write tests that use / are interested only in a subset of the provided shared resource


## Shared resource

The example will use the following shared resource definition:

```scala
final case class  Thingie()

final case class Flooble(value: Int)

final case class SharedResource(thingie: Thingie, flooble: Flooble)

```

In a real project this might look like

```scala
final case class SharedResource(transactor: Transactor[IO], client: Client[IO], metrics: Metrics[IO]) // etc
```
, however, we will proceed with the former for simplicity.

## "Child" test suites


Here is a set of tests that are only interested in a `Thingie` resource:

```scala
import weaver.extra.res._
import cats.implicits._
import cats.effect.IO

object BarSpec extends Tests {

  val test1: RTest[Thingie] = test("the thingie thingies") { thingie =>
    IO(println("hey, testing thingie thingies")) >>
      expect(thingie == Thingie())
  }

  val test2: RTest[Thingie] = test("another thingie test") { thingie =>
    IO(println(s"got thingie: $thingie")) >> 
      expect(11==11)
  }
}
```

And here are some tests that only use `Flooble`:

```scala
import weaver.extra.res._

object FooSpec extends Tests {
  val tests: List[RTest[Flooble]] = List(
      test("the flooble floobles") { flooble =>
        expect(flooble == Flooble(42))
      }
  )
}
```


A couple of things to note:

- We can use weaver-test-extra via the single wildcard import 
`import weaver.extra.res._`
- The function ` def test[R](name: String)(run: R => IO[Expectations]): RTest[R]` returns a `weaver-test` test that has access to a resource (i.e. input argument) of type `R`
- "Child suites" are just modules / files containing test descriptions. In this example we have one module containing tests of type `RTest[Flooble]` and another module containing tests of type `RTest[Thingie]`. This organisation is completely up to you and the specifics of your project
- Child suites, being just contaners of `RTest` values, are not runnable by themselves - since we have not yet specified how to provide the resources that they use.
- When you say `object Blah extends Tests`, this is only done in order to bring into scope the implicits
necessary to write `test("blah")(r => ...)` and to use the `expect` macro. Nothing "frameworky" is going on here.

## "Parent" test suite

Now that we have our individual tests, we need a piece of code runnable by `WeaverTest`, that acquires
the necessary shared resources, and provides them to the individual tests.

```scala
import fs2.Stream
import cats.implicits._
import weaver.extra.res._
import cats.effect.{IO, Resource}

object SharedResourceSuite extends PureResourceSuite {

  override type R = SharedResource

  override def sharedResource: Resource[IO, SharedResource] = for {
    _ <- Resource.liftF(IO(println("acquiring shared resource")))
    result = SharedResource(Thingie(), Flooble(42))
  } yield result


  val fooTests: Stream[IO, RTest[SharedResource]] =
    Stream.emits(FooSpec.tests)
      .local[SharedResource](_.flooble)

  val barTests: Stream[IO, RTest[SharedResource]] =
     Stream.emits(
      List(BarSpec.test1, BarSpec.test2)
  ).local[SharedResource](_.thingie)

  override def suitesStream: fs2.Stream[IO, RTest[SharedResource]] =
    fooTests ++ barTests
}
```

Let's break this down:

1. In order to be runnable by `weaver-test`, we say `extends PureResourceSuite` in our "parent" suite

2. We need to specify the type of the shared resource:

```scala
override type R = SharedResource
```

3. We need to specify how we acquire the shared resource:

```scala
override def sharedResource: Resource[IO, SharedResource] = for {
    _ <- Resource.liftF(IO(println("acquiring shared resource")))
    result = SharedResource(Thingie(), Flooble(42))
  } yield result
```

4. If we have a function which knows how to go from `ParentResource => ChildResource`, and we have a 
stream `Stream[IO, RTest[ChildResource]]`, we can get a stream `Stream[IO, ParentResource]` using the `local` function provided by the
`weaver-test-extra` library:


```scala
implicit class RTestStreamOps[R, A](private val xs: Stream[IO, A]) {
    def local[R1](f: R1 => R)(implicit ev: A =:= RTest[R]): Stream[IO, RTest[R1]] =
      xs.map(x => Contravariant[RTest].contramap(x)(f))
  }
```  

If we dropped the implicits cruft (which is added for convenience and to help type inference), that function would look like:

```scala
    def local[R1](xs: Stream[IO, RTest[R]])(f: R1 => R): Stream[IO, RTes[R1]] =
      xs.map(x => Contravariant[RTest].contramap(x)(f))
  }
```

Digging further, `RTest` is defined as 

```scala
final case class RTest[R](name: String, run: (Log[IO], R) => IO[Expectations])
```

Which for the purposes of the discussion can be simplified to 

```scala
final case class RTest[R](run: R => IO[Expectations])
```

Which is just a function `R => IO[Expectations])` polymorhic in `R`.

So, all this is a really long way of saying `local` uses the fact that functions form a contravariant functor on their input.


5. Using the `local` function, we can compose the values provided by the child suites into a stream of tests of the shared resource:

```scala
 val fooTests: Stream[IO, RTest[SharedResource]] =
    Stream.emits(FooSpec.tests)
      .local[SharedResource](_.flooble)

  val barTests: Stream[IO, RTest[SharedResource]] =
     Stream.emits(
      List(BarSpec.test1, BarSpec.test2)
  ).local[SharedResource](_.thingie)

  override def suitesStream: fs2.Stream[IO, RTest[SharedResource]] =
    fooTests ++ barTests
```