package com.dimitarg.example

import fs2.Stream
import weaver.pure._
import cats.effect.{IO, Resource}
import scala.concurrent.ExecutionContext
import java.util.concurrent.Executors

object ExampleResSuite extends Suite {

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

  val suites: List[String] => Stream[IO, Test] = lines => Stream(
    pureTest("the file has one line") {
      expect(lines.size == 1)
    },
    pureTest("the file has the expected content") {
      expect(lines == List("Hello, there!"))    
    }
  )

  override def suitesStream: fs2.Stream[IO, Test] =
    Stream.resource(sharedResource).flatMap { res =>
      suites(res)
    }
  
}
