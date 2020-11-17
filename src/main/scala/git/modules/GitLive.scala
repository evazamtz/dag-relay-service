package git

import java.net.URLEncoder

import config._
import domain._
import sttp.client._
import sttp.model.StatusCode
import zio._
import io.circe._
import io.circe.generic.auto._
import io.circe.syntax._
import zio.logging._
import zio.sugar._
import io.circe.optics.JsonPath._
import io.circe.parser._


package object modules {

  case class GitLive(parallelism: Int, logging: Logger[String]) extends git.Service {

    implicit val backend:SttpBackend[Identity, Nothing, NothingT] = HttpURLConnectionBackend()

    case class Action(action:String, file_path: String,  content: Option[String])
    case class CommitPayload(branch: String, commit_message: String, actions: Seq[Action])
    case class GitRequest(uri: String, branch: String, headers: Map[String, String])

    def syncDags(project: Project, dags: Map[DagName, DagPayload]) : Task[Unit] = for {
      actions <- ZIO.collectParN(parallelism)(dags.toSeq) {
        case (name, payload) => (for {
           file   <- fetchRawFile(project, name)
           action <- createAction(project, name, payload, file) getOrFail(None)
        } yield action).orElseFail(None)
      }
      _ <- logging.info(actions.mkString(","))
      _ <- ZIO.when(actions.nonEmpty) { sendCommitRequest(project, actions) }
    } yield ()


    override def desyncDags(project: Project, dags: Seq[DagName]): Task[Unit] = for {
      actions <- ZIO.foreachPar(dags)(dagName => ZIO.succeed(createDeleteAction(project, dagName)))
        _ <- ZIO.when(actions.nonEmpty) { sendCommitRequest(project, actions) }
    } yield ()

    def createDeleteAction(project: Project, dagName: DagName) = Action("delete", createFilePath(project, dagName), Option.empty)

    def createAction(project: Project, dagName: DagName, dagPayload: DagPayload, file: Response[String]) : Option[Action] = {
      val filePath = createFilePath(project, dagName)
      val found = file.code != StatusCode.NotFound
      if (!found) Option(Action("create", filePath, Option(dagPayload)))
       else {
        if (file.body != dagPayload) Option(Action("update", filePath, Option(dagPayload)))
        else Option.empty
      }
    }


    def sendCommitRequest(project: Project, actions : Seq[Action]): Task[Response[String]] = {
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

    def fetchRawFile(project: Project, name: DagName): Task[Response[String]] =  logging.info(s"${createPathForRawFile(project, name)}/raw?ref=${project.git.branch}") *>  Task {
      quickRequest
        .headers(createHeaders(project.git))
        .contentType("application/json")
        .get(uri"${createPathForRawFile(project, name)}/raw?ref=${project.git.branch}")
        .send[Identity]()
    }

    def fetchFiles(project: Project) : Task[Response[String]] = {
      val path = createPathForListOfFiles(project)

      logging.info(path) *> Task {
        quickRequest
          .headers(createHeaders(project.git))
          .contentType("application/json")
          .get(uri"${path}")
          .send[Identity]()
      }
    }

    def createPathForRawFile(project: Project, dagName: DagName): String = s"${project.git.repository}/files/${URLEncoder.encode(s"${project.git.path}/${project.name}_${dagName}.yaml", "UTF-8")}"

    def createPathForCommitPush(project: Project): String = s"${project.git.repository}/commits"

    def createPathForListOfFiles(project: Project): String = s"${project.git.repository}/tree?path=${project.git.path}"

    def createCommitPayload(project: Project, actions: Seq[Action]): CommitPayload = CommitPayload(project.git.branch, createCommitMsg(project), actions)

    def createFilePath(project:Project, dagName: DagName): String = s"${project.git.path}/${project.name}_${dagName}.yaml"

    def createCommitMsg(project: Project): String = s"from ${project.name}"

    def createHeaders(gitRepoSettings: GitRepoSettings): Map[String, String] = Map("PRIVATE-TOKEN" -> gitRepoSettings.privateToken)

    override def getNamesByProject(project: Project): Task[Seq[DagName]] = for {
      response <- fetchFiles(project)
      json     <- ZIO.fromEither(parse(response.body))
      names    <-  getNamesFromResponse(project, json)
      _        <- logging.info(names.mkString(","))
    } yield names


    def getNamesFromResponse(project: Project, result: Json): Task[Seq[DagName]] = Task {
      val list: List[String] = root.each.name.string.getAll(result)
      list.map(name => parseFileName(project, name))
    }

    def parseFileName(project: Project, name: String): DagName = name.substring(project.name.length + 1, name.length - 5)
  }
}
