package weaver.extra

import cats.Contravariant
import fs2.Stream
import cats.effect.IO
import weaver.{Expectations, Log}

package object res {

  def loggedTest[R](name: String)(run: (Log[IO], R) => IO[Expectations]): RTest[R] = RTest(name, run)

  def test[R](name: String)(run: R => IO[Expectations]): RTest[R] = RTest(name, (_, r) => run(r))


  implicit class RTestStreamOps[R, A](private val xs: Stream[IO, A]) {
    def local[R1](f: R1 => R)(implicit ev: A =:= RTest[R]): Stream[IO, RTest[R1]] =
      xs.map(x => Contravariant[RTest].contramap(x)(f))
  }
}
