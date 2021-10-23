package weaver.pure

import cats.effect.IO
import weaver.Expectations
import weaver.TestName

final case class Test(name: TestName, run: IO[Expectations])
