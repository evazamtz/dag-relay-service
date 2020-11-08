package git

import java.net.URLEncoder

import domain._
import sttp.client._
import sttp.model.StatusCode
import zio._
import io.circe._
import io.circe.generic.auto._
import io.circe.syntax._
import zio.logging._


package object modules {

  class GitLive(parallelism: Int, logging: Logger[String]) extends git.Service {

    implicit val backend:SttpBackend[Identity, Nothing, NothingT] = HttpURLConnectionBackend()

    case class LocalDag(dagName: DagName, dagPayload: DagPayload)

    case class Action(action: String, file_path: String, content: String)
    case class CommitPayload(branch: String, commit_message: String, actions: Seq[Action])
    case class GitRequest(uri: String, branch: String, headers: Map[String, String])

    def syncDags(project: Project, dags: Map[DagName, DagPayload]) : Task[Unit] = for {
      actions <- ZIO.collectParN(parallelism)(dags.toSeq) {
        case (name, payload) => (for {
           file   <- getRawFile(project, name)
           action <- ZIO.fromOption[Action](createAction(project, name, payload, file))
        } yield action).orElseFail(None)
      }
      _ <- logging.info(actions.mkString(","))
      _ <- ZIO.when(actions.nonEmpty) { createCommit(project, actions) }
    } yield ()


    def createAction(project: Project, dagName: DagName, dagPayload: DagPayload, file: Response[String]) : Option[Action] = {
      val filePath = createFilePath(project, dagName)
      val found = file.code != StatusCode.NotFound
      if (!found) Some(Action("create", filePath, dagPayload))
       else {
        if (file.body != dagPayload) Some(Action("update", filePath, dagPayload))
        else None
      }
    }


    def createCommit(project: Project, actions : Seq[Action]): Task[Response[String]] = {
      val path = createPathForCommitPush(project)

      logging.info(path) *> Task {
        quickRequest
          .headers(createHeaders(project.git))
          .contentType("application/json")
          .body(createCommitPayload(project, actions).asJson.noSpaces)
          .post(uri"${path}")
          .send[Identity]()
      }
    }

    def getRawFile(project: Project, name: DagName): Task[Response[String]] =  logging.info(s"${createPathForRawFile(project, name)}/raw?ref=${project.git.branch}") *>  Task {
      quickRequest
        .headers(createHeaders(project.git))
        .contentType("application/json")
        .get(uri"${createPathForRawFile(project, name)}/raw?ref=${project.git.branch}")
        .send[Identity]()
    }

    def createPathForRawFile(project: Project, dagName: DagName): String = s"${project.git.repository}/files/${URLEncoder.encode(s"${project.git.path}/${project.name}_${dagName}.yaml", "UTF-8")}"

    def createPathForCommitPush(project: Project): String = s"${project.git.repository}/commits"

    def createCommitPayload(project: Project, actions: Seq[Action]): CommitPayload = CommitPayload(project.git.branch, createCommitMsg(project), actions)

    def createFilePath(project:Project, dagName: DagName): String = s"${project.git.path}/${project.name}_${dagName}.yaml"

    def createCommitMsg(project: Project): String = s"from ${project.name}"

    def createHeaders(gitRepoSettings: GitRepoSettings): Map[String, String] = Map("PRIVATE-TOKEN" -> gitRepoSettings.privateToken)
  }
}
