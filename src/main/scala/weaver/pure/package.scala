package weaver

import cats.Contravariant
import fs2.Stream
import cats.effect.IO
import cats.implicits._
import cats.effect.Resource

package object pure {

  def rTest[R](name: String)(run: R => IO[Expectations]): RTest[R] = RTest(name, run)

  def test(name: String)(run: IO[Expectations]): Test = Test(name, run)

  implicit class RTestStreamOps[R, A](private val xs: Stream[IO, A]) {

    def local[R1](f: R1 => R)(implicit ev: A =:= RTest[R]): Stream[IO, RTest[R1]] =
      xs.map(x => Contravariant[RTest].contramap(x)(f))

    def provide(r: R)(implicit ev: A =:= RTest[R]): Stream[IO, Test] = {
      xs.local[Unit](_ => r).map(x => Test(x.name, x.run(())))
    }

    def provideResource(r: Resource[IO, R])(implicit ev: A =:= RTest[R]): Stream[IO, Test] =
      Stream.resource(r).flatMap(xs.provide(_))
  }

  def expect: Expect = new Expect
  def assert: Expect = new Expect

  implicit def expectationsConversion(e: Expectations): IO[Expectations] =
    e.pure[IO]
}
