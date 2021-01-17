package weaver

import cats.effect.IO
import cats.implicits._

package object pure {

  def test(name: String)(run: IO[Expectations]): Test = Test(name, run)

  def expect: Expect = new Expect
  def assert: Expect = new Expect

  implicit def expectationsConversion(e: Expectations): IO[Expectations] =
    e.pure[IO]
}
