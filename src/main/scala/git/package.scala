import domain.{Dag, GitRepoSettings}
import zio.{Has, Task, ULayer, ZLayer}

import domain.{Dag, GitRepoSettings}
import git.modules.GitLive
import zio.{Task, _}



package object git {
  type Git = Has[Service]

  trait Service {
    def syncDag(dag: Dag, gitRepoSettings: GitRepoSettings): Task[Unit]
  }

  val dummy: ULayer[Git] = ZLayer.succeed(new Service {
    override def syncDag(dag: Dag, gitRepoSettings: GitRepoSettings): Task[Unit] = Task.unit
  })

  val live: ZLayer[Any, Nothing, Git] = ZLayer.succeed(new GitLive)
}