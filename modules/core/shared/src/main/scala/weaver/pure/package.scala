package weaver

import cats.implicits._
import cats.effect.IO
import fs2.Stream
import cats.Parallel
import cats.Applicative

package object pure extends Expectations.Helpers {

  import util._

  def test(
      name: String
  )(run: IO[Expectations])(implicit loc: SourceLocation): IO[Test] = {
    failureToExpectations(run)
      .map(x => Test(name, x))
  }

  def pureTest(name: String)(run: => Expectations)(implicit
      loc: SourceLocation
  ): Test = Test(name, run)

  def parSuite[F[_]: Parallel](tests: List[F[Test]]): Stream[F, Test] =
    Stream.evals(tests.parTraverse(identity))

  def seqSuite[F[_]: Applicative](tests: List[F[Test]]): Stream[F, Test] =
    Stream.evals(tests.sequence[F, Test])
}
