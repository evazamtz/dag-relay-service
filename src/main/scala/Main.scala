import cli.CLI
import zio._
import zio.console._
import zio.logging._

object Main extends App {
  def run(args: List[String]): zio.URIO[zio.ZEnv, ExitCode] = {
    val logic = ZIO.runtime[ZEnv].flatMap(implicit runtime => for {
      cli  <- ZIO.access[CLI](_.get)
      exit <- cli.run(args)
    } yield exit)

    // inject dependencies
    val logger       = Logging.console()
    val gitLiveLayer = (logger ++ config.live) >>> git.liveFromConfig
    val layers       = config.live ++ storage.modules.inMemory ++ gitLiveLayer ++ crawler.dummy ++ logger ++ cli.live ++ httpServer.live

    val program = logic.provideSomeLayer[ZEnv](layers)

    // folding errors to exit codes
    program.foldCauseM(
      err      => putStrLn(err.prettyPrint).as(ExitCode.failure),
      exitCode => ZIO.succeed(exitCode)
    )
  }
}