package weaver.pure

import cats.Contravariant
import cats.effect.IO
import weaver.{Expectations, Log}

final case class RTest[R](name: String, run: (Log[IO], R) => IO[Expectations])

object RTest {
  implicit val contravariantForRTest: Contravariant[RTest[*]] = new Contravariant[RTest] {
    override def contramap[A, B](fa: RTest[A])(f: B => A): RTest[B] = RTest(
      fa.name, (log, r) => fa.run(log, f(r))
    )
  }
}
