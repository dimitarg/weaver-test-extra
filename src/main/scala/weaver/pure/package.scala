package weaver

import cats.implicits._
import cats.effect.IO
import fs2.Stream

package object pure extends Expectations.Helpers {

  def test(name: String)(run: IO[Expectations])(implicit loc: SourceLocation): IO[Test] = {
    failureToExpectations(run)
      .map(x => Test(name, () => x))
  }

  def pureTest(name: String)(run: => Expectations)(implicit loc: SourceLocation): Test = Test(name, () => run)

  def parSuite(tests: List[IO[Test]]): Stream[IO, Test] = Stream.evals(tests.parTraverse(identity))

  private def failureToExpectations(x: IO[Expectations])(implicit loc: SourceLocation): IO[Expectations] = {
    x.handleErrorWith { t =>
      IO.delay(t.printStackTrace()).map { _ =>
        failure(t.toString())
      }
    }
  }
}
