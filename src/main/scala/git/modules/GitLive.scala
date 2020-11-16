package git

import java.net.URLEncoder

import domain._
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

    override def syncDags(project:Project, dags:Map[DagName,DagPayload]): Task[Unit] = for {
      _ <- ZIO.unit
      dags2 = dags map { case ((n,p)) => Dag(project.name, n, p) }
      _ <- ZIO.foreach_(dags2) { dag => processFile(createRequest(dag, project.git)) }
    } yield ()

    case class GitRequest(uri: String, branch: String, commit_message: String, content: String, headers: Map[String, String])

    def createRequest(dag: Dag, gitRepoSettings: GitRepoSettings): GitRequest = {
      val path = createPath(dag, gitRepoSettings)

      GitRequest(
        s"${gitRepoSettings.repository}/$path",
        gitRepoSettings.branch,
        createCommitMsg(dag),
        dag.payload,
        createHeaders(gitRepoSettings)
      )
    }

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

    def createPath(dag: Dag, gitRepoSettings: GitRepoSettings): String = URLEncoder.encode(s"${gitRepoSettings.path}/${dag.project}_${dag.name}.yaml", "UTF-8")

    def createCommitMsg(dag: Dag): String = s"from ${dag.project}"

    def createHeaders(gitRepoSettings: GitRepoSettings): Map[String, String] = Map("PRIVATE-TOKEN" -> gitRepoSettings.privateToken)
  }
}
