package weaver

import cats.Contravariant
import fs2.Stream
import cats.effect.IO
import cats.implicits._
import weaver.{Expectations, Log}

package object pure {

  def loggedRTest[R](name: String)(run: (Log[IO], R) => IO[Expectations]): RTest[R] = RTest(name, run)

  def loggedTest(name: String)(run: (Log[IO]) => IO[Expectations]): RTest[Unit] = RTest(name, (log, _) => run(log))

  def rTest[R](name: String)(run: R => IO[Expectations]): RTest[R] = RTest(name, (_, r) => run(r))

  def test(name: String)(run: IO[Expectations]): RTest[Unit] = RTest(name, (_, _) => run)

  def tests[A](xs: RTest[A]*): Stream[IO, RTest[A]] = 
    Stream.apply(xs: _*)

  implicit class RTestStreamOps[R, A](private val xs: Stream[IO, A]) {
    def local[R1](f: R1 => R)(implicit ev: A =:= RTest[R]): Stream[IO, RTest[R1]] =
      xs.map(x => Contravariant[RTest].contramap(x)(f))
  }

  def expect: Expect = new Expect
  def assert: Expect = new Expect

  implicit def singleExpectationConversion(e: SingleExpectation)(implicit loc: SourceLocation): IO[Expectations] =
    Expectations.fromSingle(e).pure[IO]

  implicit def expectationsConversion(e: Expectations): IO[Expectations] =
    e.pure[IO]
}
