package com.dimitarg.example

import fs2.Stream
import weaver.pure._
import cats.effect.{IO, Resource}
import scala.concurrent.ExecutionContext
import java.util.concurrent.Executors
import cats.effect.Blocker

object ExampleResSuite extends Suite {

  val sharedResource: Resource[IO, List[String]] = for {
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

  val suites: List[String] => Stream[IO, Test] = lines => Stream(
    test("the file has one line") {
      expect(lines.size == 1)
    },
    test("the file has the expected content") {
      expect(lines == List("Hello, there!"))    
    }
  )

  override def suitesStream: fs2.Stream[IO, Test] =
    Stream.resource(sharedResource).flatMap { res =>
      suites(res)
    }
  
}
