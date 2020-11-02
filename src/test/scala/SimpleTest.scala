import HelloWorld._
import zio._
import zio.console._
import zio.test.Assertion._
import zio.test._
import zio.test.environment._

object HelloWorld {
  def sayHello: URIO[Console, Unit] =
    console.putStrLn("Hello, World!")
}

object HelloWorldSpec extends DefaultRunnableSpec {
  def spec = suite("HelloWorldSpec")(
    testM("sayHello correctly displays output") {
      for {
        _      <- sayHello
        output <- TestConsole.output
      } yield assert(output)(equalTo(Vector("Hello, Wцorld!\n")))
    }
  )
}

object SimpleTestSpec extends DefaultRunnableSpec {
  def spec = suite("learning tests")(
    testM("omg brah") {
      for {
        x <- ZIO.succeed(42)
      } yield assert(x)(equalTo(42))
    }
  )
}