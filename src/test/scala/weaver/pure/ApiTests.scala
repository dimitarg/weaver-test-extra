package weaver.pure

import cats.effect.IO
import fs2.Stream
import scala.util.control.NoStackTrace

object ApiTest extends Suite {

  override def suitesStream: Stream[IO, Test] = parSuite(
    List(
      test("`test` raises IO errors as Expectations failures") {
        val tst = test("something")(IO.raiseError(SomeError))
        tst.map { result =>
          expect(
            result.run.run.isInvalid
          ) // as opposed to this raising an io error and failing the test
        }
      }
    )
  )

  final case object SomeError extends Exception with NoStackTrace
}
