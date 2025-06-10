package com.dimitarg.example

import cats.effect.{IO, Resource}
import fs2.Stream
import weaver.pure._

object ExampleResSuite extends Suite {

  // shared resource
  final case class TextFile(lines: List[String])

  // describe how to acquire shared resource
  val sharedResource: Resource[IO, TextFile] = {
    val xs = fs2.io.readInputStream(
      IO(getClass().getResourceAsStream("/foo.txt")),
      1024,
      closeAfterUse = true
    )
    Resource.eval(
      xs.through(fs2.text.utf8.decode).through(fs2.text.lines).compile.toList
    ).map(TextFile(_))
  }

  // suite which uses shared resource
  val suites: TextFile => Stream[IO, Test] = textFile =>
    Stream(
      pureTest("the file has one line") {
        expect(textFile.lines.size == 1)
      },
      pureTest("the file has the expected content") {
        expect(textFile.lines == List("Hello, there!"))
      }
    )

  // construct `suitesStream` by acquiring resource and passing that to your `suite` via `flatMap`
  override def suitesStream: Stream[IO, Test] =
    Stream.resource(sharedResource).flatMap { res =>
      suites(res)
    }

}
