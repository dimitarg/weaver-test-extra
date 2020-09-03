package weaver.pure

import cats.effect.{IO, Resource}
import fs2.Stream
import weaver.{PureIOSuite, Test, TestOutcome}

trait ResourceSuite extends PureIOSuite {
  type R
  def sharedResource : Resource[IO, R]

  def suitesStream: Stream[IO, RTest[R]]

  def maxParallelism : Int = 10000

  override def spec(args: List[String]): Stream[IO, TestOutcome] = {
    val parallism = math.max(1, maxParallelism)
    val mkResource = Stream.resource(sharedResource)
    mkResource.flatMap { res =>
      suitesStream.parEvalMap(parallism) { test =>
        Test(test.name, log => test.run(log, res))
      }
    }
  }
}