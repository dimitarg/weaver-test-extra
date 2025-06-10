package weaver.pure

import scala.concurrent.duration._

import cats.effect.IO
import cats.effect.kernel.{Ref, Resource}
import fs2.Stream

object ResourceHandlingTest extends Suite {

  override def suitesStream: Stream[IO, Test] =
    Stream.resource(SomeService.make).flatMap { service =>
      Stream.eval(
        test("Stream lifecycle is correctly handled") {
          service.doSomething.as(success)
        }
      )
    }
}

final case class SomeService(released: Ref[IO, Boolean]) {

  val latency = 3.seconds

  val doSomething: IO[Unit] = IO.sleep(latency) >> released.get.ifM(
    IO.unit,
    IO.raiseError(
      new RuntimeException("SomeService already released. Cannot do things")
    )
  )
}

object SomeService {
  val make: Resource[IO, SomeService] = Resource
    .make {
      Ref.of[IO, Boolean](true)
    } { ref =>
      ref.set(false)
    }
    .map(SomeService(_))
}
