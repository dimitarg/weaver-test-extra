package weaver

import cats.Contravariant
import fs2.Stream
import cats.effect.IO
import cats.implicits._

package object pure {

  def rTest[R](name: String)(run: R => IO[Expectations]): RTest[R] = RTest(name, run)

  def test(name: String)(run: IO[Expectations]): RTest[Unit] = RTest(name, _ => run)

  implicit class RTestStreamOps[R, A](private val xs: Stream[IO, A]) {
    def using[R1](f: R1 => R)(implicit ev: A =:= RTest[R]): Stream[IO, RTest[R1]] =
      xs.map(x => Contravariant[RTest].contramap(x)(f))
  }

  def expect: Expect = new Expect
  def assert: Expect = new Expect

  implicit def pureSingleExpect(e: SingleExpectation)(implicit loc: SourceLocation): IO[Expectations] =
    Expectations.fromSingle(e).pure[IO]

  implicit def pureExpect(e: Expectations): IO[Expectations] =
    e.pure[IO]
}
