package weaver.pure

import cats.effect.Timer
import cats.effect.ContextShift
import cats.effect.IO
import cats.effect.ConcurrentEffect

trait IOSupport {
  val ec = scala.concurrent.ExecutionContext.global
  implicit def timer : Timer[IO] = IO.timer(ec)
  implicit def cs : ContextShift[IO] = IO.contextShift(ec)
  implicit def effect : ConcurrentEffect[IO] = IO.ioConcurrentEffect
}
