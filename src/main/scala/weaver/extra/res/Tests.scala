package weaver.extra.res

import cats.implicits._
import cats.effect.{ContextShift, IO, Timer}
import weaver.{Expect, Expectations, SingleExpectation, SourceLocation}

import scala.concurrent.ExecutionContext

trait Tests {
  def expect: Expect = new Expect
  def assert: Expect = new Expect

  def ec: ExecutionContext = ExecutionContext.global
  implicit val shift: ContextShift[IO] = IO.contextShift(ec)
  implicit val timer: Timer[IO] = IO.timer(ec)

  implicit def singleExpectationConversion(e: SingleExpectation)(implicit loc: SourceLocation): IO[Expectations] =
    Expectations.fromSingle(e).pure[IO]

  implicit def expectationsConversion(e: Expectations): IO[Expectations] =
    e.pure[IO]
}
