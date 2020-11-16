package cli.modules

import cli.CliDependencies
import com.monovore.decline._
import com.monovore.decline.effect._
import domain.ProjectName
import httpServer.HttpServer
import zio.interop.catz.taskConcurrentInstance
import zio._
import zio.console._
import cats.implicits._
import crawler.Crawler
import git.Git
import zio.logging.Logging
import zio.sugar._

class LiveCLI extends cli.Service {
  override def run(input: List[String]): RIO[CliDependencies, zio.ExitCode] = {
    CommandIOApp.run(command, input)
      .catchAll(t => putStrLnErr(t.getMessage).as(cats.effect.ExitCode.Error))
      .map(c => zio.ExitCode(c.code))
  }

  case class Daemon()
  case class ListProjects()
  case class SyncProjects(projects: List[ProjectName], all: Boolean)

  private val command = Command[RIO[CliDependencies, cats.effect.ExitCode]](
    name   = "drs",
    header = "DAG Relay Service",
  ) {
    val daemonOpts: Opts[Daemon] = Opts.subcommand("daemon", "Start daemon"){ Opts.unit.map( _ => Daemon()) }
    val listProjectsOpts: Opts[ListProjects] = Opts.subcommand("listProjects", "List all projects"){ Opts.unit.map( _ => ListProjects()) }

    val syncProjectsOps: Opts[SyncProjects] = Opts.subcommand("syncProjects", "Sync projects"){
      val projects = Opts.options[ProjectName](long = "project", short = "p", help = "Project name", metavar = "project").orEmpty
      val allFlag  = Opts.flag(long = "all", short = "a", help = "Sync all projects").orFalse

      (projects, allFlag).mapN(SyncProjects)
    }

    val program = (daemonOpts orElse listProjectsOpts orElse syncProjectsOps).map {
      case Daemon() => for {
        server <- ZIO.access[HttpServer](_.get)
        exit   <- server.run
      } yield exit

      case ListProjects() => for {
        projects <- storage.getProjects
        _        <- ZIO.foreach(projects.toList)(proj => {
          putStrLn(
            s"""
              |Project: ${proj._1}
              |  Token: ${proj._2.token}
              |  Fetch endpoint: ${proj._2.fetchEndpoint}
              |  Git:
              |    Repository: ${proj._2.git.repository}
              |    Branch: ${proj._2.git.branch}
              |    Path: ${proj._2.git.path}
              |
              |""".stripMargin
          )
        })
      } yield ExitCode.success

      case SyncProjects(projectList, all) => for {
        _           <- ZIO.ensureOrFail(all || projectList.nonEmpty, new Throwable("Provide project list or --all flag"))
        allProjects <- storage.getProjects
        projects    <- if (all) ZIO.succeed(allProjects.values)
                       else ZIO.foreach(projectList)(p => allProjects.get(p) getOrFail new Throwable( s"Project $p not found"))
        git         <- ZIO.access[Git](_.get)
        logger      <- ZIO.access[Logging](_.get)
        crawler     <- ZIO.access[Crawler](_.get)
        _           <- ZIO.foreach(projects.toList)(p => for {
          _    <- logger.info(s"Syncing project ${p.name}")
          dags <- crawler.fetch(p)
          _    <- git.syncDags(p, dags)
          _    <- logger.info(s"Synced ${dags.size} dags in project ${p.name}")
        } yield ())
        _           <- logger.info(s"Synced ${projects.size} project(s)")
      } yield ExitCode.success
    }

    program.map(z => z.map(c => cats.effect.ExitCode(c.code)))
  }
}
