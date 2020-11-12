package crawler

import domain._
import io.circe.parser.decode
import sttp.client._
import sttp.model.StatusCode
import zio.{Task, ZIO}


package object modules {
  case class CrawlerException(status:StatusCode,
                              statusText:String,
                              projectName: ProjectName) extends Exception(
    s"""error for the project $projectName,
       | statusCode: $status
       | error: $statusText""".stripMargin)
  class CrawlerLive extends crawler.Service {

    implicit val backend: SttpBackend[Identity, Nothing, NothingT] = HttpURLConnectionBackend()

    override def fetch(project: Project): Task[Map[DagName, DagPayload]] = for {
      dagsStr    <- getDagsFromUrl(project.name, project.fetchEndpoint)
      found      = dagsStr.code == StatusCode.Ok
      dags       <- if (!found) Task.fail(CrawlerException(dagsStr.code, dagsStr.statusText, project.name)) else ZIO.fromEither(getDagsMap(dagsStr.body))
    } yield (dags)

    def getDagsFromUrl(projectName: ProjectName, fetchEndpoint: String): Task[Response[String]] = Task {
      quickRequest
        .contentType("application/json")
        .get(uri"${fetchEndpoint}/${projectName}")
        .send[Identity]()
    }

    def getDagsMap(dag: DagMap) = {
      decode[Map[DagName, DagPayload]](dag)
}
  }

}
