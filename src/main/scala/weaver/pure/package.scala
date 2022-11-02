package weaver

import cats.effect.IO

package object pure extends Expectations.Helpers {

  def test(name: String)(run: IO[Expectations])(implicit loc: SourceLocation): IO[Test] = {
    failureToExpectations(run)
      .map(x => Test(name, () => x))
  }

  def pureTest(name: String)(run: => Expectations)(implicit loc: SourceLocation): Test = Test(name, () => run)

  private def failureToExpectations(x: IO[Expectations])(implicit loc: SourceLocation): IO[Expectations] = {
    x.handleErrorWith { t =>
      IO.delay(t.printStackTrace()).map { _ =>
        failure(t.toString())
      }
    }
  }
}
