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

  class GitLive(parallelism: Int) extends git.Service {

    implicit val backend:SttpBackend[Identity, Nothing, NothingT] = HttpURLConnectionBackend()

    case class Dag(dagName: DagName, dagPayload: DagPayload)
    case class Action(action: String, file_path: String, content: String)
    case class CommitPayload(branch: String, commit_message: String, actions: Seq[Action])
    case class GitRequest(uri: String, branch: String, headers: Map[String, String])

    def syncDags(project: Project, dags: Map[DagName, DagPayload]) : Task[Unit] = for {

      actions <- ZIO.foreachParN(parallelism)(dags.map(dag => Dag(dag._1, dag._2)))(dag =>
         for {
            file     <- getRawFile(project, dag)
            filePath = createFilePath(project, dag)
            found    = file.code != StatusCode.NotFound
            action   <- if(!found) ZIO.succeed(Action("create", filePath, dag.dagPayload))
                        else {
                           if(file.body != dag.dagPayload) ZIO.succeed(Action("update", filePath, dag.dagPayload))
                           else ZIO.unit
                        }
          } yield action
      )
      filteredActions:Seq[Action] =  actions.filter(!_.isInstanceOf[Action])
      _ <- createCommit(project, filteredActions)
    } yield ()


    def createCommit(project: Project, actions : Seq[Action]): Task[Response[String]] = Task {
      quickRequest
        .headers(createHeaders(project.git))
        .contentType("application/json")
        .body(createCommitPayload(project, actions).asJson.noSpaces)
        .post(uri"${createPathForCommitPush(project)}")
        .send[Identity]()
    }

    def getRawFile(project: Project, dag: Dag): Task[Response[String]] = Task {
      quickRequest
        .headers(createHeaders(project.git))
        .contentType("application/json")
        .get(uri"${createPathForRawFile(project, dag)}/raw?ref=${project.git.branch}")
        .send[Identity]()
    }

    def createPathForRawFile(project: Project, dag: Dag): String = s"${project.git.repository}/files/${URLEncoder.encode(s"${project.git.path}/${project.name}_${dag.dagName}.yaml", "UTF-8")}"

    def createPathForCommitPush(project: Project): String = s"${project.git.repository}/commits"

    def createCommitPayload(project: Project, actions: Seq[Action]): CommitPayload = CommitPayload(project.git.branch, createCommitMsg(project), actions)

    def createFilePath(project:Project, dag: Dag): String = s"${project.git.path}/${project.name}_${dag.dagName}.yaml"

    def createCommitMsg(project: Project): String = s"from ${project.name}"

    def createHeaders(gitRepoSettings: GitRepoSettings): Map[String, String] = Map("PRIVATE-TOKEN" -> gitRepoSettings.privateToken)
  }
}
