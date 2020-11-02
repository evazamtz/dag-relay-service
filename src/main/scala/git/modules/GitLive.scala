package git

import java.net.URLEncoder

import domain.{Dag, GitRepoSettings}
import sttp.client._
import sttp.model.StatusCode
import zio._
import io.circe._
import io.circe.generic.auto._
import io.circe.syntax._

package object modules {

  class GitLive extends git.Service {

    // TODO: вынести в зависимость
    implicit val backend:SttpBackend[Identity, Nothing, NothingT] = HttpURLConnectionBackend()

    override def syncDag(dag: Dag, gitRepoSettings: GitRepoSettings): Task[Unit] = for {
      request <- createRequest(dag, gitRepoSettings)
      _       <- processFile(request)
    } yield ()

    case class GitRequest(uri: String, branch: String, commit_message: String, content: String, headers: Map[String, String])

    def createRequest(dag: Dag, gitRepoSettings: GitRepoSettings): Task[GitRequest] = for {
      commitMsg <- createCommitMsg(dag)
      headers   <- createHeaders(gitRepoSettings)
      path      <- createPath(dag, gitRepoSettings)
    } yield GitRequest(
      s"${gitRepoSettings.repository}/$path",
      gitRepoSettings.branch,
      commitMsg,
      dag.payload,
      headers
    )

    def processFile(request: GitRequest): Task[Unit] = for {
        file     <- getRawFile(request)
        found     = file.code != StatusCode.NotFound
        _ <- ZIO.when(!found)(createFile(request))
        _ <- ZIO.when(found && file.body != request.content)(updateFile(request))
    } yield ()

    def createFile(request: GitRequest): Task[Response[String]] = Task {
      quickRequest
        .headers(request.headers)
        .contentType("application/json")
        .body(request.asJson.noSpaces)
        .post(uri"${request.uri}")
        .send[Identity]()
    }

    def updateFile(request: GitRequest): Task[Response[String]] = Task {
      quickRequest
        .headers(request.headers)
        .contentType("application/json")
        .body(request.asJson.noSpaces)
        .put(uri"${request.uri}")
        .send[Identity]()
    }


    def getRawFile(request: GitRequest): Task[Response[String]] = Task {
      quickRequest
        .headers(request.headers)
        .contentType("application/json")
        .get(uri"${request.uri}/raw?ref=${request.branch}")
        .send[Identity]()
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
