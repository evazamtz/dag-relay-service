import domain.{Dag, GitRepoSettings, Project}
import zio.{Has, Ref, Task, ZIO, ZLayer}
import io.circe._
import io.circe.generic.auto._
import io.circe.syntax._
import sttp.client3._

import scala.util.Try


package object git {
  type Git = Has[Service]

  trait Service {
    def syncDag(dag: Dag, gitRepoSettings: GitRepoSettings): Task[String]
  }


  case class GitRequest(uri: String, payload: Payload, headers: Map[String, String])
  case class Payload(branch: String, commit_message: String, actions: Seq[Action])
  case class Action(action: String, file_path: String, content: String)



  def createRequest(dag: Dag, gitRepoSettings: GitRepoSettings): Task[GitRequest] = {

    val request = for {
      actions <- ZIO.succeed(Seq(Action("create", s"${gitRepoSettings.path}\\${dag.project}\\${dag.name}.yaml", raw"${dag.payload}")))
      payload <- ZIO.succeed(Payload(gitRepoSettings.branch, s"from ${dag.project}", actions))
      request <- Task {
        {
          GitRequest(
            gitRepoSettings.repository,
            payload,
            Map("Content-Type" -> "application/json", "PRIVATE-TOKEN" -> gitRepoSettings.privateToken),
          )
        }
      }
    } yield request

    request
  }

  val live: ZLayer[Any, Nothing, Git] = ZLayer.fromEffect(

    ZIO.succeed {
      new Service {
        def syncDag(dag: Dag, gitRepoSettings: GitRepoSettings): Task[String] = {

          val backend = HttpURLConnectionBackend()

          for {
            request <- createRequest(dag, gitRepoSettings)
            response <- Task
            {
               quickRequest
                .headers(request.headers)
                .contentType("application/json")
                .body(request.payload.asJson.noSpaces)
                .post(uri"${request.uri}")
                .send(backend)
            }
          } yield (response.body)
        }
      }
    })

}