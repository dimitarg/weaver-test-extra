package com.dimitarg.example.anyjunk.test

import weaver.pure._
import fs2._
import cats.effect.IO
import cats.effect.Resource
import scala.concurrent.duration._

object SharedResources extends Suite {

  override def suitesStream: Stream[IO,Test] =
    Stream.resource(setup.mkAll).flatMap { resources =>
      Stream(
        DbSuite.tests(resources.transactor),
        HttpSuite.tests(resources.client),
        RestApiSuite.tests(resources)
      ).parJoinUnbounded
    }


  object DbSuite {
    case class TestData()

    def tests(tx: Transactor): Stream[IO, Test] = {
      Stream.resource[IO, TestData](Resource.pure(TestData())).flatMap { _ =>
        Stream(
          test("database store test") {
            val _ = tx
            IO.unit.as(expect(true))
          }
        )
      }.timeout(5.seconds) 
    }
  }


  object HttpSuite {
    def tests(client: HttpClient): Stream[IO, Test] = Stream(
      test("http client test") {
        val _ = client
        IO.unit.as(expect(true))
      }
    )
  }

  object RestApiSuite {
    def tests(resources: TestResources): Stream[IO, Test] = {
      val _ = resources
      Stream[IO, Test](
        test("REST API test 1") {
          IO.unit.as(expect(true))
        },

        test("REST API test 2") {
          IO.unit.as(expect(true))
        }
      )
    }.timeout(10.seconds)
  }
  
}
