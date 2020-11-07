import domain._
import git.modules.GitLive
import zio._



package object git {
  type Git = Has[Service]

  trait Service {
    def syncDags(project: Project, dags:Map[DagName,DagPayload]): Task[Unit]
  }

  val dummy: ULayer[Git] = ZLayer.succeed(_: Map[DagName, DagPayload] => ZIO.unit)

  val live: ZLayer[Any, Nothing, Git] = ZLayer.succeed(new Service {
    override def syncDags(project: Project, dags: Map[DagName, DagPayload]): Task[Unit] = for
  })
}