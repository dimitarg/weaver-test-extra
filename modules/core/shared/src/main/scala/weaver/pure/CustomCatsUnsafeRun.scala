package weaver.pure

import weaver.*
import scala.concurrent.Future

import cats.effect.{FiberIO, IO}

object CatsUnsafeRun extends weaver.CatsUnsafeRun

trait CustomCatsUnsafeRun extends UnsafeRun[IO] with CustomCatsUnsafePlatformCompat {

  type CancelToken = FiberIO[Unit]

  override implicit val parallel = IO.parallelForIO
  override implicit val effect = IO.asyncForIO

  def cancel(token: CancelToken): Unit = unsafeRunSync(token.cancel)

  def unsafeRunAndForget(task: IO[Unit]): Unit = task.unsafeRunAndForget()
  def unsafeRunToFuture(task: IO[Unit]): Future[Unit] = task.unsafeToFuture()

}
