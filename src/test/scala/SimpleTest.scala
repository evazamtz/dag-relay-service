import org.http4s._
import org.http4s.dsl.Http4sDsl
import zio._
import zio.test.Assertion._
import zio.test._
import zio.interop.catz._

object SimpleTestSpec extends DefaultRunnableSpec {
  def spec = suite("learning tests")(
    testM("omg brah") {
      for {
        x <- ZIO.succeed(42)
      } yield assert(x)(equalTo(42))
    }
  )
}

object RoutesTest extends DefaultRunnableSpec {
  private val dsl = Http4sDsl[Task]

  def spec = suite("testing http api")(
    testM("GET /ping") {
      for {
        api      <- api.buildRoutes(dsl).provideLayer(storage.modules.inMemory ++ git.dummy)
        response <- api.run(Request[Task](Method.GET, Uri(path="/ping")))
        body     <- response.body.through(fs2.text.utf8Decode).compile.string
      } yield assert(body)(equalTo("pong"))
    },

    testM("GET /projects") {
      for {
        api      <- api.buildRoutes(dsl).provideLayer(storage.modules.inMemory ++ git.dummy)
        response <- api.run(Request[Task](Method.GET, Uri(path="/projects")))
        body     <- response.body.through(fs2.text.utf8Decode).compile.string
      } yield assert(body)(equalTo("""{"projects":["core"]}"""))
    },

    testM("Fail on no token provided with 400 Bad Request") {
      for {
        api      <- api.buildRoutes(dsl).provideLayer(storage.modules.inMemory ++ git.dummy)
        response <- api.run(Request[Task](Method.POST, Uri(path="/projects/kek/dags/omg")))
      } yield assert(response.status.code)(equalTo(400))
    }


  )
}