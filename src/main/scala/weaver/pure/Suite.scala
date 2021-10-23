package weaver.pure

import cats.effect.IO
import fs2.Stream
import weaver.{Test => WeaverTest, TestOutcome, Expectations, BaseCatsSuite, EffectSuite, CatsUnsafeRun, EffectCompat}

trait Suite extends EffectSuite[IO] with BaseCatsSuite with Expectations.Helpers {

  def suitesStream: Stream[IO, Test]

  def maxParallelism : Int = 10000

  override def spec(args: List[String]): Stream[IO, TestOutcome] = {
    val parallism = math.max(1, maxParallelism)
    suitesStream.parEvalMap(parallism) { test =>
        WeaverTest(test.name, test.run)
    }
  }

  override implicit protected def effectCompat: EffectCompat[IO] = CatsUnsafeRun

  override def getSuite: EffectSuite[IO] = this
}