package com.dimitarg.example.sharedres

import cats.implicits._
import weaver.pure._

object FooSuite extends IOSupport {
  val tests: List[RTest[FooResource]] = List(
    loggedRTest("the foo foos") { (log, foo) =>
      log.info("starting the foo foos") >>
        expect(foo == FooResource())

    }
  )
}
