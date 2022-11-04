package weaver.pure

import cats.effect.IO
import fs2.Stream
import weaver._
import util._

trait Suite extends EffectSuite[IO] with BaseCatsSuite with Expectations.Helpers {

  def suitesStream: Stream[IO, Test]

  override def spec(args: List[String]): Stream[IO, TestOutcome] = {
    suitesStream.map(toTestOutcome)
  }

  override implicit protected def effectCompat: EffectCompat[IO] = CatsUnsafeRun

  override def getSuite: EffectSuite[IO] = this
}