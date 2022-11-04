package weaver

import cats.implicits._
import cats.effect.IO
import fs2.Stream

package object pure extends Expectations.Helpers {
  
  import util._

  def test(name: String)(run: IO[Expectations])(implicit loc: SourceLocation): IO[Test] = {
    failureToExpectations(run)
      .map(x => Test(name, x))
  }

  def pureTest(name: String)(run: => Expectations)(implicit loc: SourceLocation): Test = Test(name, run)

  def parSuite(tests: List[IO[Test]]): Stream[IO, Test] = Stream.evals(tests.parTraverse(identity))

}
