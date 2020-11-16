import zio._
import cli.modules.LiveCLI
import config.Config
import crawler.Crawler
import git.Git
import httpServer.HttpServer
import storage.Storage
import zio.logging.Logging

package object cli {
  type CLI = Has[Service]
  type CliDependencies = ZEnv with Crawler with Config with Storage with Git with HttpServer with Logging

  trait Service {
    def run(input: List[String]): RIO[CliDependencies, ExitCode]
  }

  val live: ULayer[CLI] = ZLayer.succeed(new LiveCLI)
}
