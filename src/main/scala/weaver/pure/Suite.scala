package weaver.pure

import cats.effect.IO
import fs2.Stream
import weaver.{Test => WeaverTest, TestOutcome, BaseIOSuite, RunnableSuite, TestName, Expectations}

trait Suite extends RunnableSuite[IO] with BaseIOSuite with Expectations.Helpers {

  def suitesStream: Stream[IO, Test]

  def maxParallelism : Int = 10000

  override def spec(args: List[String]): Stream[IO, TestOutcome] = {
    val parallism = math.max(1, maxParallelism)
    suitesStream.parEvalMap(parallism) { test =>
        WeaverTest(test.name, test.run)
    }
  }

  override def plan: List[TestName] = Nil
}