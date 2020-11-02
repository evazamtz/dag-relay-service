import java.net.URLEncoder

import domain.{Dag, GitRepoSettings, Project}
import zio.{Task, _}
import io.circe._
import io.circe.generic.auto._
import io.circe.syntax._

import sttp.client._
import sttp.model.StatusCode



package object git {
  type Git = Has[Service]

  trait Service {
    def syncDag(dag: Dag, gitRepoSettings: GitRepoSettings): Task[Unit]
  }

  case class GitRequest(uri: String, branch: String, commit_message: String, content: String, headers: Map[String, String])

  val live: ZLayer[Any, Nothing, Git] = ZLayer.fromEffect(

    ZIO.succeed {
      new Service {

        def syncDag(dag: Dag, gitRepoSettings: GitRepoSettings): Task[Unit] = {
          for {
            request <- createRequest(dag, gitRepoSettings)
            _       <- processFile(request)
          } yield ()
        }

        def createRequest(dag: Dag, gitRepoSettings: GitRepoSettings): Task[GitRequest] = for {
          commitMsg <- createCommitMsg(dag)
          headers   <- createHeaders(gitRepoSettings)
          path      <- createPath(dag, gitRepoSettings)
          request   <- Task {
            GitRequest(
              s"${gitRepoSettings.repository}/$path",
              gitRepoSettings.branch,
              commitMsg,
              dag.payload,
              headers
            )
          }
        } yield request

        def processFile(request: GitRequest) : Task[Unit] =  {
          for {
            file <- getRawFile(request)
            _    <- if (file.code == StatusCode.NotFound) createFile(request)
                    else {
                      if (file.body != request.content) updateFile(request)
                      else ZIO.unit
                    }
          } yield ()
        }

        def createFile(request: GitRequest) : Task[Response[_]] = Task {

          implicit val backend = HttpURLConnectionBackend()
          quickRequest
            .headers(request.headers)
            .contentType("application/json")
            .body(request.asJson.noSpaces)
            .post(uri"${request.uri}")
            .send()
        }

        def updateFile(request: GitRequest): Task[Response[_]] = Task {

          implicit val backend = HttpURLConnectionBackend()

          quickRequest
            .headers(request.headers)
            .contentType("application/json")
            .body(request.asJson.noSpaces)
            .put(uri"${request.uri}")
            .send()
        }

        def getRawFile(request: GitRequest): Task[Response[_]] = Task {
          implicit val backend = HttpURLConnectionBackend()

          quickRequest
            .headers(request.headers)
            .contentType("application/json")
            .get(uri"${request.uri}/raw?ref=${request.branch}")
            .send()
        }


        def createPath(dag: Dag, gitRepoSettings: GitRepoSettings): UIO[String] = ZIO.succeed {
          URLEncoder.encode(s"${gitRepoSettings.path}/${dag.project}_${dag.name}.yaml", "UTF-8")
        }

        def createCommitMsg(dag: Dag): UIO[String] = ZIO.succeed(s"from ${dag.project}")

        def createHeaders(gitRepoSettings: GitRepoSettings): UIO[Map[String, String]] = ZIO.succeed(
          Map("PRIVATE-TOKEN" -> gitRepoSettings.privateToken)
        )
      }
    }
  )

}
