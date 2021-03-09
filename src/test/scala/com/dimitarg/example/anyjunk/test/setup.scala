package com.dimitarg.example.anyjunk.test

import cats.effect.Resource
import cats.effect.IO

final case class Transactor()
final case class HttpClient()
  
final case class RedisContainer()
final case class PostgresContainer()

final case class TestContainers(redis: RedisContainer, postgres: PostgresContainer)

final case class TestResources(
  transactor: Transactor, client: HttpClient, containers: TestContainers
)

object setup {
  
  def mkAll: Resource[IO, TestResources] = Resource.pure[IO, TestResources](
    TestResources(
      Transactor(), HttpClient(),
      TestContainers(RedisContainer(), PostgresContainer())
    )
  )
}
