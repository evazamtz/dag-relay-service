package crawler.modules

import crawler.Service
import domain.{DagName, DagPayload, Project}
import io.circe.generic.codec.DerivedAsObjectCodec.deriveCodec
import io.circe.parser.decode
import sttp.client._
import sttp.model.StatusCode
import zio.sugar._
import zio.{Task, ZIO}

class HttpCrawler extends Service {

  implicit val backend: SttpBackend[Identity, Nothing, NothingT] = HttpURLConnectionBackend()

  case class DagsResult(dags: Map[DagName,DagPayload])

  case class FetcherException(error: String, project: Project, response: Response[String])
    extends Exception(s"$error: ${project.fetchEndpoint} resulted in a ${response.code} ${response.statusText}")

  override def fetch(project: Project): Task[Map[DagName, DagPayload]] = for {
    response <- makeRequest(project.fetchEndpoint)
    _        <- ZIO.ensureOrFail(response.code == StatusCode.Ok, FetcherException(s"Fetch attempt failed", project, response))
    payload  = response.body
    result   <- ZIO.fromEither(decode[DagsResult](payload)).mapError(e => new Error(s"Dags parse attempt failed: ${e.getMessage}", e.getCause))
  } yield result.dags

  protected def makeRequest(endpoint: String): Task[Response[String]] = Task {
    quickRequest
      .contentType("application/json")
      .get(uri"${endpoint}")
      .send[Identity]()
  }
}
