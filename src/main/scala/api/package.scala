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


package object api {

  def buildRoutes(dsl:Http4sDsl[Task]):URIO[Storage, HttpApp[Task]] = for {
    storage <- ZIO.access[Storage](_.get)
  } yield routes(dsl, storage)

  protected def routes(dsl:Http4sDsl[Task], repo: storage.Service):HttpApp[Task] = {
    import dsl._

    val service = HttpRoutes
      .of[Task] {

        case GET -> Root / "ping" => Ok("pong")
        case GET -> Root / "projects" => for {
          projects <- repo.getProjects
          response <- Ok(Json.obj("projects" -> projects.asJson).noSpaces)
        } yield response

      }.orNotFound
    service
  }
}


