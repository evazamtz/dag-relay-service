import java.nio.file.Path

import cats.effect.ConcurrentEffect
import config.Config
import zio.{Task, _}
import zio.console.{putStrLn, _}
import zio.interop.catz._
import zio.interop.catz.implicits._
import org.http4s._
import org.http4s.dsl.Http4sDsl
import org.http4s.implicits._
import org.http4s.server.blaze.BlazeServerBuilder
import zio.logging._


object Main extends App {

  private val dsl = Http4sDsl[Task]

  def run(args: List[String]): zio.URIO[zio.ZEnv, ExitCode] = {

    val logic = ZIO.runtime[ZEnv].flatMap(implicit runtime => for { // хитрая хуйня для имплиситрв cats.effect.{ConcurrentEffect,Timer}
      appConf <- config.app
      httpApp <- api.buildRoutes(dsl)
      apiConf = appConf.api
      exit    <- BlazeServerBuilder[Task](runtime.platform.executor.asEC)
        .bindHttp(apiConf.port, apiConf.host)
        .withHttpApp(httpApp)
        .resource
        .toManagedZIO
        .useForever
    } yield exit)

    // inject dependencies
    val logger  = Logging.console()

    val gitLiveLayer = logger >>> git.live(3)
    val layers  = config.live ++ storage.modules.inMemory ++ gitLiveLayer ++ crawler.dummy ++ logger
    val program = logic.provideSomeLayer[ZEnv](layers)

    // folding errors to exit codes
    program.foldCauseM(
      err => putStrLn(err.prettyPrint).as(ExitCode.failure),
      _ => ZIO.succeed(ExitCode.success)
    )
  }
}