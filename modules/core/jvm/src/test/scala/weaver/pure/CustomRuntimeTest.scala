package weaver.pure

import cats.effect.IO
import fs2._
import cats.implicits._
import cats.effect.unsafe.IORuntime
import cats.effect.unsafe.IORuntimeBuilder
import scala.concurrent.ExecutionContext
import java.util.concurrent.Executors
import java.util.concurrent.ThreadFactory

object CustomRuntimeTest extends Suite {

  val expectedThreadName = "Custom name"
  override implicit val theIORuntime: IORuntime = {

    val threadPool = Executors.newCachedThreadPool(new ThreadFactory {
      override def newThread(r: Runnable): Thread = new Thread(r, expectedThreadName);
    })
    val ex = ExecutionContext.fromExecutor(threadPool)

    IORuntimeBuilder()
      .setCompute(ex, () => threadPool.shutdown())
      .build()
  }
  override def suitesStream: Stream[IO, Test] = parSuite(
    List(
      test("custom compute") {
        IO.delay(Thread.currentThread().getName())
          .map(name => expect(name === expectedThreadName))
        IO.pure(success)
      }
    )
  )
}
