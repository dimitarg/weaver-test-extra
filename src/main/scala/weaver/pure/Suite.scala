package weaver.pure

import cats.effect.IO
import fs2.Stream
import weaver.{Test => WeaverTest, TestOutcome, Expectations, BaseCatsSuite, EffectSuite,
CatsUnsafeRun, EffectCompat, Filters}

trait Suite extends EffectSuite[IO] with BaseCatsSuite with Expectations.Helpers {

  def suitesStream: Stream[IO, Test]

  def maxParallelism : Int = 10000

  override def spec(args: List[String]): Stream[IO, TestOutcome] = {
    val parallism = math.max(1, maxParallelism)
    val filter = Filters.filterTests(this.name)(args)
    suitesStream
      .filter(x => filter(x.name))
      .parEvalMap(parallism) { test =>
        WeaverTest(test.name.name, test.run)
      }
  }

  override implicit protected def effectCompat: EffectCompat[IO] = CatsUnsafeRun

  override def getSuite: EffectSuite[IO] = this
}