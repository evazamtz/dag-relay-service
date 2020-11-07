package crawler

import domain.{DagName, DagPayload, Project}
import io.circe.parser.decode
import sttp.client.{HttpURLConnectionBackend, Identity, NothingT, Response, SttpBackend, quickRequest, _}
import sttp.model.StatusCode
import zio.{Task, ZIO}


package object modules {

  class CrawlerLive extends crawler.Service {

    implicit val backend: SttpBackend[Identity, Nothing, NothingT] = HttpURLConnectionBackend()

    override def fetch(project: Project): Task[Map[DagName, DagPayload]] = for {
      dagsStr    <- getDagsFromUrl(project)
      found      = dagsStr.code != StatusCode.NotFound
      dags       <- if (!found) Task.fail(new Exception("Page not found")) else ZIO.fromEither(getDagsMap(dagsStr.body))
    } yield (dags)

    def getDagsFromUrl(project: Project): Task[Response[String]] = Task {
      quickRequest
        .contentType("application/json")
        .get(uri"${project.fetchEndpoint}/${project.name}")
        .send[Identity]()
    }

    def getDagsMap(string: String) = {
      decode[Map[DagName, DagPayload]](string)
}


  }

}
