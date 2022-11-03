package com.dimitarg.example.traced

import scala.jdk.CollectionConverters._

import scala.concurrent.duration._
import cats.~>
import cats.arrow.FunctionK
import cats.data.ReaderT
import cats.implicits._
import cats.effect.{IO, Temporal, Resource}
import fs2._
import natchez.{Trace, Span}
import natchez.Trace.kleisliInstance

import weaver.pure._
import weaver.pure.traced._
import natchez.EntryPoint
import com.dimitarg.example.util.IntegrationTestConfig

object ExampleTracedSuite extends Suite {

    override def suitesStream: Stream[IO,Test] =
    Stream.resource(makeEntryPoint.flatMap(_.root("ExampleTracedSuite"))).flatMap { implicit rootSpan =>
      val service = SomeTracedService.apply[App]
      tracedParSuite("Service tests")(serviceTests(service)) ++
        tracedSeqSuite("Some other tests")(someOtherTests) ++
        tracedParSuite("Test errors")(failingTests)
    }

  def serviceTests(service: SomeTracedService[App]): List[TracedTest] = List(
    tracedTest("SomeTracedService.foo test") { span =>
      service.translate(provideSpan(span)).foo
        .as(success) 
    },
    tracedTest("SomeTracedService.bar test") { span =>
      service.translate(provideSpan(span)).bar
        .as(success) 
    }
  )

  def someOtherTests: List[TracedTest] = List(
    tracedTest("some other test 1") { _ =>
      expect(1 === 1).pure[IO]
    },
    tracedTest("some other test 2") { span => 
      span.put("app.important.info" -> 42)
        .as(expect(2 === 2))
    }
  )

  def failingTests: List[TracedTest] = List(
    tracedTest("fail with IO error") { _ =>
      IO.raiseError(new RuntimeException("failed I have"))
    },
    tracedTest("fail with expectation failure") { _ =>
      {expect(1 === 2) and expect(6 === 18)}.pure[IO]
    }
  )

  private val makeEntryPoint: Resource[IO, EntryPoint[IO]] = for {
    testConfig <- Resource.eval(IntegrationTestConfig.load)
    result <- testConfig match {
      case IntegrationTestConfig.CI(hcKey) =>
        natchez.honeycomb.Honeycomb.entryPoint[IO](
          service = "weaver-test-extra-tests"
         ) { builder =>
          IO.delay {
            builder
              .setDataset("weaver-test-extra-tests")
              .setWriteKey(hcKey)
              .setGlobalFields(
                Map(
                  "service_name" -> "weaver-test-extra-tests",
                ).asJava
              )
              .build
          }
        }
      case IntegrationTestConfig.NotCI =>
        natchez.noop.NoopEntrypoint.apply[IO]().pure[Resource[IO, *]]
    }
  } yield result

  type App[A] = ReaderT[IO, Span[IO], A]

  def provideSpan(span: Span[IO]): App ~> IO = {
    def provide[A](x: App[A]): IO[A] = x.run(span)
    FunctionK.lift(provide)
  }

}

sealed trait SomeTracedService[F[_]] {
  val foo: F[Unit]
  val bar: F[Unit]

  def translate[G[_]](fg: F ~> G): SomeTracedService[G] = {
    val underlying = this

    new SomeTracedService[G] {

      override val foo: G[Unit] = fg(underlying.foo)

      override val bar: G[Unit] = fg(underlying.bar)
    }
  }
}

object SomeTracedService {
  def apply[F[_]: Trace: Temporal] = new SomeTracedService[F] {

    override val foo: F[Unit] = Trace[F].span("SomeTracedService.foo") {
      for {
        _ <- Temporal[F].sleep(10.millis)
      } yield ()
    }

    override val bar: F[Unit] = Trace[F].span("SomeTracedService.bar") {
      for {
        _ <- Temporal[F].sleep(50.millis)
      } yield ()
    }
  }
}
