package weaver

import cats.Contravariant
import fs2.Stream
import cats.effect.IO
import cats.implicits._
import cats.effect.Resource

package object pure {

  def rTest[R](name: String)(run: R => IO[Expectations]): RTest[R] = RTest(name, run)

  def test(name: String)(run: IO[Expectations]): RTest[Unit] = RTest(name, _ => run)

  implicit class RTestStreamOps[R, A](private val xs: Stream[IO, A]) {

    def using[R1](f: R1 => R)(implicit ev: A =:= RTest[R]): Stream[IO, RTest[R1]] =
      xs.map(x => Contravariant[RTest].contramap(x)(f))

    def provideShared(r: R)(implicit ev: A =:= RTest[R]): Stream[IO, RTest[Unit]] = {
      xs.using(_ => r)
    }

    def provide(r: Resource[IO, R])(implicit ev: A =:= RTest[R]): Stream[IO, RTest[Unit]] =
      Stream.resource(r).flatMap(xs.provideShared(_))
  }

  def expect: Expect = new Expect
  def assert: Expect = new Expect

  implicit def expectationsConversion(e: Expectations): IO[Expectations] =
    e.pure[IO]
}
