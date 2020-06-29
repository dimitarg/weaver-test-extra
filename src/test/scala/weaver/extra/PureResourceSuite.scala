package weaver.extra

import fs2.Stream
import cats.effect.{IO, Resource}
import weaver.extra.PureResourceSuite.Test
import weaver.{Test => WTest}
import weaver.{Expectations, Log, PureIOSuite, TestOutcome}

trait PureResourceSuite extends PureIOSuite {
  type Res
  def sharedResource : Resource[IO, Res]

  def test(name: String)(run: (Log[IO], Res) => IO[Expectations]): Test[Res] = Test(name, run)

  def suitesStream: Stream[IO, Test[Res]]

  def maxParallelism : Int = 10000

  override def spec(args: List[String]): Stream[IO, TestOutcome] = {
    val parallism = math.max(1, maxParallelism)
    val mkResource = Stream.resource(sharedResource)
    mkResource.flatMap { res =>
      suitesStream.parEvalMap(parallism) { test =>
        WTest(test.name, log => test.run(log, res))
      }
    }
  }
}

object PureResourceSuite {
  final case class Test[R](name: String, run: (Log[IO], R) => IO[Expectations])

}
