import domain._
import git.modules.GitLive
import zio.{Task, _}



package object git {
  type Git = Has[Service]

  trait Service {
    def syncDags(project: Project, dags:Map[DagName,DagPayload]): Task[Unit]
  }

  val dummy: ULayer[Git] = ZLayer.succeed(_ => Task.unit)

  val live: ZLayer[Any, Nothing, Git] = ZLayer.succeed(new GitLive)
}