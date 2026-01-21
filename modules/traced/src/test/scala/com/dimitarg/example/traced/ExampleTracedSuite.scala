package com.dimitarg.example.traced

import scala.jdk.CollectionConverters._

import scala.concurrent.duration._
import cats.~>
import cats.implicits._
import cats.effect.{IO, Temporal, Resource}
import fs2._
import org.typelevel.otel4s.trace.Tracer
import org.typelevel.otel4s.Attribute
import org.typelevel.otel4s.oteljava.OtelJava

import weaver.pure._
import weaver.pure.traced._
import com.dimitarg.example.util.IntegrationTestConfig

object ExampleTracedSuite extends Suite {

  override def suitesStream: Stream[IO, Test] =
    Stream
      .resource {
        for {
          testConfig <- Resource.eval(IntegrationTestConfig.load)
          tracer <- testConfig match {
            case config: IntegrationTestConfig.CI =>
              mkHoneycombTracer(config)
            case IntegrationTestConfig.NotCI =>
              Tracer.noop[IO].pure[Resource[IO, *]]
          }
        } yield tracer
      }
      .flatMap { implicit tracer =>
        val service = SomeTracedService.apply[IO]

        rootTracedSuite("ExampleTracedSuite") {
          tracedParSuite("Service tests")(serviceTests(service)) ++
            tracedSeqSuite("Some other tests")(someOtherTests)
        }
      }

  def serviceTests(service: SomeTracedService[IO])(implicit tracer: Tracer[IO]): List[IO[Test]] = List(
    tracedTest("SomeTracedService.foo test") {
      service.foo
        .as(success)
    },
    tracedTest("SomeTracedService.bar test") {
      service.bar
        .as(success)
    }
  )

  def someOtherTests(implicit tracer: Tracer[IO]): List[IO[Test]] = List(
    tracedTest("some other test 1") {
      expect(1 === 1).pure[IO]
    },
    tracedTest("some other test 2") {
      tracer.currentSpanOrThrow
        .flatMap(_.addAttribute(Attribute("app.important.info", 42.toLong)))
        .as(expect(2 === 2))
    }
  )

  def mkTracer(
      serviceName: String,
      staticFields: Map[String, String],
      otlpConfig: OtlpConfig
  ): Resource[IO, Tracer[IO]] = {

    OtelJava
      .autoConfigured[IO] { builder =>
        builder.addPropertiesSupplier { () =>
          Map(
            // this one is probably redundant - should be the case we're not using the global otel instance
            "otel.java.global-autoconfigure.enabled" -> true.show,
            "otel.service.name" -> serviceName,
            "otel.resource.attributes" -> renderAttributes(staticFields),
            "otel.exporter.otlp.endpoint" -> otlpConfig.endpoint,
            "otel.exporter.otlp.headers" -> renderAttributes(otlpConfig.headers),
            // default configuration of otel uses BatchSpanProcessor which keeps spans in a bounded queue
            // the default is 2048 elements which will cause span loss
            "otel.bsp.max.queue.size" -> 1073741824.show // 2 ^ 30

          ).asJava
        }
      }
      .evalMap { otel =>
        otel.tracerProvider.get("com.dimitarg.example.traced")
      }
  }

  def mkHoneycombTracer(
      config: IntegrationTestConfig.CI
  ): Resource[IO, Tracer[IO]] = {
    val otlpConfig = OtlpConfig(
      endpoint = "https://api.honeycomb.io",
      headers = Map(
        "x-honeycomb-team" -> config.honeycombWriteKey
      )
    )
    mkTracer(
      serviceName = config.serviceName,
      staticFields = Map(),
      otlpConfig = otlpConfig
    )
  }

  private def renderAttributes(attrs: Map[String, String]): String =
    attrs.toList
      .map { case (k, v) => s"$k=$v" }
      .mkString(",")

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
  def apply[F[_]: Tracer: Temporal] = new SomeTracedService[F] {

    override val foo: F[Unit] = Tracer[F].span("SomeTracedService.foo").surround {
      for {
        _ <- Temporal[F].sleep(10.millis)
      } yield ()
    }

    override val bar: F[Unit] = Tracer[F].span("SomeTracedService.bar").surround {
      for {
        _ <- Temporal[F].sleep(50.millis)
      } yield ()
    }
  }
}

final case class OtlpConfig(
    endpoint: String,
    headers: Map[String, String]
)
