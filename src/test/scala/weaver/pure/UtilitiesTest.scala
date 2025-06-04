package weaver.pure

import cats.implicits._
import cats.effect.IO
import fs2._

import util._

object UtilitiesTest extends Suite {

  override def suitesStream: Stream[IO, Test] = Stream(
    pureTest("outcomeToString contains source location and assertion failure body") {

      val test = Test("some test", expect(clue(1 === 2)))
      val testOutcome = toTestOutcome(test)
      val asString = outcomeToString(testOutcome)

      println(asString)
      expect(asString.contains("UtilitiesTest.scala")) and
        expect(asString.contains("1 === 2"))

    }
  )

}
