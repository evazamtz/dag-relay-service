import config.Config
import domain.{Dag, GitRepoSettings}
import git.Git
import zio._
import zio.interop.catz._
import zio.interop.catz.implicits._
import org.http4s._
import org.http4s.dsl.Http4sDsl
import org.http4s.implicits._
import storage.Storage
import io.circe._
import io.circe.generic.auto._
import io.circe.syntax._


package object api {

  def buildRoutes(dsl:Http4sDsl[Task]):URIO[Storage with Config with Git, HttpApp[Task]] = {

    import dsl._

    for {
      repo    <- ZIO.access[Storage](_.get)
      appConf <- ZIO.access[Config](_.get)
      git     <- ZIO.access[Git](_.get)

      service = HttpRoutes.of[Task]
        {

          case GET -> Root / "ping" => Ok("pong")
          case GET -> Root / "projects" => for {
            projects <- repo.getProjects
            appConfig <- appConf.app
            response <- Ok(Json.obj("projects" -> projects.asJson, "host" ->  appConfig.api.host.asJson).noSpaces)
          } yield response
          case GET -> Root / "pushTest" => {

            val source = scala.io.Source.fromFile("yaml.yaml")
            val content = try {
                source.getLines.mkString
              } finally source.close()

            val testDag = Dag("project", "test", content)
            val testRepositorySettings = GitRepoSettings(
              "https://gitlab.pimpay.ru/api/v4/projects/294/repository/files",
              "master",
              "dags",
              "RtPpsq7iiFv2xQiDdU8J"
            )

            git.syncDag(testDag, testRepositorySettings) *> Ok("kukujopa")
          }

        }.orNotFound

    } yield service
  }
}
