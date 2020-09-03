package com.dimitarg.example

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

  override def suitesStream: fs2.Stream[IO,RTest[R]] = tests(
      rTest("the file has one line") { lines =>
        expect(lines.size == 1)
      },
      rTest("the file has the expected content") { lines =>
        expect(lines == List("Hello, there!"))    
      }
  )
  
}
