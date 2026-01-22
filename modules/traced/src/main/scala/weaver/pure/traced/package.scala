package weaver.pure

import cats.effect.IO
import fs2.Stream

import weaver.{SourceLocation, Expectations}
import util._
import org.typelevel.otel4s.trace.Tracer
import org.typelevel.otel4s.Attribute
import org.typelevel.otel4s.trace.SpanOps

package object traced {

  def tracedTest(name: String)(
      run: IO[Expectations]
  )(implicit loc: SourceLocation, tracer: Tracer[IO]): IO[Test] =
    tracer
      .span(name)
      .surround(test(name)(run))
      .flatTap(traceExpectationFailures)

  private def traceExpectationFailures(x: Test)(implicit tracer: Tracer[IO]): IO[Unit] =
    x.run.run.fold(
      _ =>
        tracer.currentSpanOrNoop.flatMap(
          _.addAttribute(Attribute("test.expectation.error", outcomeToString(toTestOutcome(x))))
        ),
      _ => IO.unit
    )

  def tracedParSuite(name: String)(suite: List[IO[Test]])(implicit
      tracer: Tracer[IO]
  ): Stream[IO, Test] =
    tracedSuite(weaver.pure.parSuite)(name)(suite)

  def tracedSeqSuite(name: String)(suite: List[IO[Test]])(implicit
      tracer: Tracer[IO]
  ): Stream[IO, Test] =
    tracedSuite(weaver.pure.seqSuite)(name)(suite)

  def tracedSuite(
      toStream: List[IO[Test]] => Stream[IO, Test]
  )(name: String)(suite: List[IO[Test]])(implicit tracer: Tracer[IO]): Stream[IO, Test] =
    Stream.resource(tracer.span(name).resource).flatMap { case SpanOps.Res(_, trace) =>
      toStream(suite.map(trace(_)))
    }

  def rootTracedSuite(name: String)(suite: Stream[IO, Test])(implicit tracer: Tracer[IO]): Stream[IO, Test] =
    Stream.resource(tracer.rootSpan(name).resource).flatMap { case SpanOps.Res(_, trace) =>
      suite.translate(trace)
    }

}
