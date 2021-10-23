package weaver

import cats.effect.IO
import cats.implicits._

package object pure extends Expectations.Helpers {

  def test(name: String)(run: IO[Expectations])(implicit loc: SourceLocation): Test = Test(name, run)

  def pureTest(name: String)(run: => Expectations)(implicit loc: SourceLocation): Test = test(name)(run.pure[IO])

}
