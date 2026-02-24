package weaver.pure

import cats.effect.IO
import cats.effect.unsafe.IORuntime

trait CustomCatsUnsafePlatformCompat { self: CustomCatsUnsafeRun =>

  implicit val ioRuntime: IORuntime

  def unsafeRunSync(task: IO[Unit]): Unit = ???

  def background(task: IO[Unit]): CancelToken = ???

}
