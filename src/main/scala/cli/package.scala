import zio._
import cli.modules.LiveCLI
import config.Config
import crawler.Crawler
import git.Git
import httpServer.HttpServer
import storage.Storage

package object cli {
  type CLI = Has[Service]
  type Dependencies = ZEnv with Crawler with Config with Storage with Git with HttpServer

  trait Service {
    def run(input: List[String]): RIO[Dependencies, ExitCode]
  }

  val live: ULayer[CLI] = ZLayer.succeed(new LiveCLI)
}
