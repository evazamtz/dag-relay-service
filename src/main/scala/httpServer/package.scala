import crawler.Crawler
import org.http4s.dsl.Http4sDsl
import org.http4s.server.blaze.BlazeServerBuilder
import storage.Storage
import config.Config
import git.Git
import zio._
import zio.interop.catz._
import zio.interop.catz.implicits._

package object httpServer {
  type HttpServer = Has[Service]

  trait Service {
    def run: RIO[ZEnv with Crawler with Config with Storage with Git, ExitCode]
  }

  val live: ULayer[HttpServer] = ZLayer.succeed(new Service {
    private val dsl = Http4sDsl[Task]

    override def run: RIO[ZEnv with Crawler with Config with Storage with Git, ExitCode] = {
      val program = ZIO.runtime[ZEnv].flatMap(implicit runtime => for { // хитрая хуйня для имплиситрв cats.effect.{ConcurrentEffect,Timer}
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

      program
    }
  })
}
