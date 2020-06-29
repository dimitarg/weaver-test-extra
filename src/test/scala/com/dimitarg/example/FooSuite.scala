package com.dimitarg.example

import cats.implicits._
import weaver.extra.res._

object FooSuite extends Tests {
  val tests: List[RTest[FooResource]] = List(
    loggedTest("the foo foos") { (log, foo) =>
      log.info("starting the foo foos") >>
        expect(foo == FooResource())

    }
  )
}
