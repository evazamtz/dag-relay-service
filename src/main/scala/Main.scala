import org.http4s._
import org.http4s.dsl._
import org.http4s.implicits._
import org.http4s.server.Router
import org.http4s.server.blaze.BlazeServerBuilder
import zio._
import zio.interop.catz._
import zio.interop.catz.implicits._

object Main extends CatsApp {

  def routes: HttpRoutes[Task] = {

    val dsl = new Http4sDsl[Task] {}
    import dsl._

    HttpRoutes.of[Task] {
      case _@GET -> Root / "dags" => Ok("ok!")
    }
  }

  val httpRoutes = Router[Task](
    "/" -> routes
  ).orNotFound

  def run(args: List[String]): URIO[ZEnv, ExitCode] = {
    BlazeServerBuilder.apply[Task](scala.concurrent.ExecutionContext.global)
      .bindHttp(9000, "0.0.0.0")
      .withHttpApp(httpRoutes)
      .serve
      .compile[Task, Task, cats.effect.ExitCode]
      .drain
      .fold(_ => ExitCode.failure, _ => ExitCode.success)
  }
}