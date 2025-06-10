package weaver.pure

import weaver.Expectations
import weaver.TestName

final case class Test(name: TestName, run: Expectations)
