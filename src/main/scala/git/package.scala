import domain.{Dag, GitRepoSettings, Project}
import zio._
import io.circe._
import io.circe.generic.auto._
import io.circe.syntax._
import sttp.client3._



package object git {
  type Git = Has[Service]

  trait Service {
    def syncDag(dag: Dag, gitRepoSettings: GitRepoSettings): Task[String]

    def createActions(dag: Dag, gitRepoSettings: GitRepoSettings) : UIO[Seq[Action]]
    def createPayload(dag: Dag, gitRepoSettings: GitRepoSettings, actions: Seq[Action]) : UIO[Payload]
    def createHeaders(gitRepoSettings: GitRepoSettings) : UIO[Map[String, String]]

    def createRequest(dag: Dag, gitRepoSettings: GitRepoSettings): Task[GitRequest] = for {
        actions <- createActions(dag, gitRepoSettings)
        payload <- createPayload(dag, gitRepoSettings, actions)
        headers <- createHeaders(gitRepoSettings)
        request <- Task {
            GitRequest(
              gitRepoSettings.repository,
              payload,
              headers
            )
        }
      } yield request
   }

  case class GitRequest(uri: String, payload: Payload, headers: Map[String, String])
  case class Payload(branch: String, commit_message: String, actions: Seq[Action])
  case class Action(action: String, file_path: String, content: String)



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

        override def createActions(dag: Dag, gitRepoSettings: GitRepoSettings): UIO[Seq[Action]] = ZIO.succeed(Seq(
          Action("create", s"${gitRepoSettings.path}\\${dag.project}_${dag.name}.yaml", raw"${dag.payload}"))
        )

        override def createPayload(dag: Dag, gitRepoSettings: GitRepoSettings, actions: Seq[Action]): UIO[Payload] = ZIO.succeed(
          Payload(gitRepoSettings.branch, s"from ${dag.project}", actions)
        )

        override def createHeaders(gitRepoSettings: GitRepoSettings): UIO[Map[String, String]] =  ZIO.succeed(
          Map("Content-Type" -> "application/json", "PRIVATE-TOKEN" -> gitRepoSettings.privateToken)
        )
      }
    }
  )

}
