import domain.Dag
import fs2.Stream
import fs2.text.utf8Encode
import git.Git
import zio._
import zio.interop.catz._
import zio.interop.catz.implicits._
import org.http4s._
import org.http4s.dsl.Http4sDsl
import org.http4s.implicits._
import storage.Storage
import io.circe._
import io.circe.generic.auto._
import io.circe.syntax._
import org.http4s.util.CaseInsensitiveString
import utils._

package object api {

  case class ApiException(status:Status, body:String) extends Exception(status.toString() + body)

  def buildRoutes(dsl:Http4sDsl[Task]):URIO[Storage with Git, HttpApp[Task]] = for {
    storage <- ZIO.access[Storage](_.get)
    git     <- ZIO.access[Git](_.get)
  } yield routes(dsl, storage, git)

  protected def routes(dsl:Http4sDsl[Task], repo: storage.Service, theGit: git.Service):HttpApp[Task] = {
    import dsl._

    val routes:PartialFunction[Request[Task], Task[Response[Task]]] = {
      case GET -> Root / "ping" => Ok("pong")

      case GET -> Root / "projects" => for {
        projects <- repo.getProjects
        response <- Ok(Json.obj("projects" -> projects.keys.asJson).noSpaces)
      } yield response

      case request @ POST -> Root / "projects" / projectName / "dags" / dag => for {
        token       <- request.headers.get(CaseInsensitiveString("X-Project-Token")) getOrFail ApiException(BadRequest, "X-Project-Name is required")
        projects    <- repo.getProjects
        project     <- projects.get(projectName) getOrFail ApiException(NotFound, s"Project $projectName was not found")
        _           <- ZIO.ensureOrFail(project.token == token, ApiException(Forbidden, "Invalid project token"))
        payload     <- request.body.through(fs2.text.utf8Decode).compile.string
        _           <- theGit.syncDag(Dag(projectName, dag, payload), project.git)
        response    <- Ok("Synced")
      } yield response


    }

    val routesWithErrorHandling = routes.andThen(r => r.catchAll {
      case e:ApiException => Task { Response[Task]( status = e.status, body=fs2.Stream(e.body).through(utf8Encode)) }
      case e:Throwable => InternalServerError(e.getMessage)
    } )

    val service = HttpRoutes.of[Task](routesWithErrorHandling)
    service.orNotFound
  }
}


