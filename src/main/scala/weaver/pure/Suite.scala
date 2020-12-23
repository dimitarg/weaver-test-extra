package weaver.pure

import cats.effect.IO
import fs2.Stream
import weaver.{PureIOSuite, Test, TestOutcome}

trait Suite extends PureIOSuite {

  def suitesStream: Stream[IO, Test]

  def maxParallelism : Int = 10000

  override def spec(args: List[String]): Stream[IO, TestOutcome] = {
    val parallism = math.max(1, maxParallelism)
    suitesStream.parEvalMap(parallism) { test =>
        Test(test.name, test.run)
    }
  }
}