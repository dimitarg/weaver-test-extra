package weaver.pure

import cats.data.ReaderT
import cats.effect.IO
import fs2.Stream
import natchez.Span

import weaver.{SourceLocation, Expectations}
import util._

package object traced {

  type TracedTest = ReaderT[IO, Span[IO], Test]

  def tracedTest(name: String)(run: Span[IO] => IO[Expectations])(implicit loc: SourceLocation): TracedTest =
    ReaderT { parent =>
      parent.span(name).use{ span =>
        weaver.pure.test(name)(run(span))
          .flatTap { test =>
            traceExpectationFailures(span, test)
          }
      }
    }

  private def traceExpectationFailures(span: Span[IO], x: Test): IO[Unit] =
    x.run.run.fold(
      _ => span.put("test.expectation.error" -> outcomeToString(toTestOutcome(x))),
      _ => IO.unit
    )

  def tracedParSuite(name: String)(suite: List[TracedTest])(implicit rootSpan: Span[IO]): Stream[IO,Test] =
    tracedSuite(weaver.pure.parSuite)(name)(suite)(rootSpan)
  
  def tracedSeqSuite(name: String)(suite: List[TracedTest])(implicit rootSpan: Span[IO]): Stream[IO,Test] =
    tracedSuite(weaver.pure.parSuite)(name)(suite)(rootSpan)
    
  def tracedSuite(toStream: List[IO[Test]] => Stream[IO, Test])(name: String)(suite: List[TracedTest]): Span[IO] => Stream[IO,Test] =
    parent => 
      Stream.resource(parent.span(name)).flatMap { suiteSpan =>
        val testsWithSpan = suite.map(_.run(suiteSpan))
        toStream(testsWithSpan)

      }
   
}
