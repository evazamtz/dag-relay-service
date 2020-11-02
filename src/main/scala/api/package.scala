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


package object api {

  class ApiException(val code:Int, val reason:String, val body:String) extends Exception(body)
  class ApiStatusException(val status:Status, body:String) extends ApiException(status.code, status.reason, body)

  implicit class StatusSyntax(status:Status) {
    def asException(body:String) = new ApiStatusException(status, body)
  }

  def buildRoutes(dsl:Http4sDsl[Task]):URIO[Storage with Git, HttpApp[Task]] = for {
    storage <- ZIO.access[Storage](_.get)
    git     <- ZIO.access[Git](_.get)
  } yield routes(dsl, storage, git)

  protected def routes(dsl:Http4sDsl[Task], repo: storage.Service, theGit: git.Service):HttpApp[Task] = {
    import dsl._

    def ensureHeader(request: Request[Task], header:String):ZIO[Any, ApiStatusException, String] = ZIO.effect {
      request.headers.get(CaseInsensitiveString(header)).get.value
    }.mapError(_ => BadRequest asException "X-Project-Name is required")

    val routes:PartialFunction[Request[Task], Task[Response[Task]]] = {
      case GET -> Root / "ping" => Ok("pong")

      case GET -> Root / "projects" => for {
        projects <- repo.getProjects
        response <- Ok(Json.obj("projects" -> projects.keys.asJson).noSpaces)
      } yield response

      case request @ POST -> Root / "projects" / projectName / "dags" / dag => for {
        token       <- ensureHeader(request, "X-Project-Token")
        projects    <- repo.getProjects
        project     <- Task { projects.apply(projectName) } mapError (_ => NotFound asException (s"Project $projectName was not found"))
        _           <- if (project.token == token) ZIO.unit else Task.fail(Forbidden asException ("Invalid project token"))
        payload     <- request.body.through(fs2.text.utf8Decode).compile.string
        _           <- theGit.syncDag(Dag(projectName, dag, payload), project.git)
        response    <- Ok(Map("project" -> projectName, "name" -> dag, "payload" -> payload).asJson.noSpaces)
      } yield response
    }

    val routesWithErrorHandling = routes.andThen(r => r.catchAll {
      case e:ApiException => Task { Response[Task]( status = Status(e.code, e.reason) , body=fs2.Stream(e.getMessage).through(utf8Encode)) }
      case e:Throwable => InternalServerError(e.getMessage)
    } )

    val service = HttpRoutes.of[Task](routesWithErrorHandling)
    service.orNotFound
  }
}


