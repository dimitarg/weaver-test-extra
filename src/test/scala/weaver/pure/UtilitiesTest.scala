package weaver.pure

import cats.implicits._
import cats.effect.IO
import fs2._

import util._

object UtilitiesTest extends Suite {

  override def suitesStream: Stream[IO, Test] = Stream(
    pureTest("outcomeToString") {

      val test = Test("some test", expect(1 === 2))
      val asString = outcomeToString(toTestOutcome(test))

      expect(asString.contains("UtilitiesTest.scala")) and
        expect(asString.contains("expect(1 === 2)"))

    }
  )

}
