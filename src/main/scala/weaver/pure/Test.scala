package weaver.pure

import cats.effect.IO
import weaver.Expectations

final case class Test(name: String, run: IO[Expectations])
