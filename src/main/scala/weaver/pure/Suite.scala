package weaver.pure

import cats.effect.{IO, Resource}

trait Suite extends RSuite {
  override type R = Unit
  override def sharedResource: Resource[IO,Unit] = Resource.pure(())
}
