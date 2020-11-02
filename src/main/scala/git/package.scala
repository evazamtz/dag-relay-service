import java.net.URLEncoder

import domain.{Dag, GitRepoSettings, Project}
import git.modules.GitLive
import zio.{Task, _}
import io.circe._
import io.circe.generic.auto._
import io.circe.syntax._
import sttp.client._
import sttp.model.StatusCode



package object git {
  type Git = Has[Service]

  trait Service {
    def syncDag(dag: Dag, gitRepoSettings: GitRepoSettings): Task[Unit]
  }

  val live: ZLayer[Any, Nothing, Git] = ZLayer.fromEffect( ZIO.succeed(new GitLive))
}
