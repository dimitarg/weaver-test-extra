package weaver.pure

import cats.effect.IO
import fs2.Stream
import weaver._
import util._
import cats.effect.unsafe.IORuntime

trait Suite extends EffectSuite[IO] with BaseCatsSuite with Expectations.Helpers {

  def suitesStream: Stream[IO, Test]

  override def spec(args: List[String]): Stream[IO, TestOutcome] = {
    suitesStream.map(toTestOutcome)
  }

  implicit val theIORuntime: IORuntime = cats.effect.unsafe.IORuntime.global

  override implicit protected val effectCompat: EffectCompat[IO] = new CustomCatsUnsafeRun {

    override implicit val ioRuntime: IORuntime = theIORuntime
  }

  override def getSuite: EffectSuite[IO] = this
}
