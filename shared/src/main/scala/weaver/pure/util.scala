package weaver.pure

import weaver.{Test => WeaverTest, Expectations, SourceLocation, TestOutcome}

import cats.effect.IO

private[pure] object util {
  def failureToExpectations(
      x: IO[Expectations]
  )(implicit loc: SourceLocation): IO[Expectations] = {
    x.handleErrorWith { t =>
      IO.delay(t.printStackTrace()).map { _ =>
        failure(t.toString())
      }
    }
  }

  def toTestOutcome(test: Test): TestOutcome =
    WeaverTest.pure(test.name.name)(() => test.run)

  def outcomeToString(x: TestOutcome): String = x.formatted(TestOutcome.Verbose)

}
