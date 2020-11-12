package crawler.modules

import crawler.Service
import domain.{DagName, DagPayload, Project}
import zio.{Task, ZIO}
import zio.sugar._
import sttp.client._
import sttp.model.StatusCode
import io.circe.parser.decode
import io.circe.generic.codec.DerivedAsObjectCodec.deriveCodec

class HttpCrawler extends Service {

  implicit val backend: SttpBackend[Identity, Nothing, NothingT] = HttpURLConnectionBackend()

  case class DagsResult(dags: Map[DagName,DagPayload])

  case class FetcherException(error: String, project: Project, response: Response[String])
    extends Exception(s"$error: ${project.fetchEndpoint} resulted in a ${response.code} ${response.statusText}")

  override def fetch(project: Project): Task[Map[DagName, DagPayload]] = for {
    response <- makeRequest(project)
    _        <- ZIO.ensureOrFail(response.code == StatusCode.Ok, FetcherException(s"Fetch attempt failed", project, response))
    payload  = response.body
    result   <- ZIO.fromEither(decode[DagsResult](payload))
  } yield result.dags

  protected def makeRequest(project: Project): Task[Response[String]] = Task {
    quickRequest
      .contentType("application/json")
      .get(uri"${project.fetchEndpoint}")
      .send[Identity]()
  }
}
