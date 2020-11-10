import config._
import domain._
import git.modules.GitLive
import zio.logging._
import zio.{Task, _}



package object git {
  type Git = Has[Service]

  trait Service {
    def syncDags(project: Project, dags:Map[DagName,DagPayload]): Task[Unit]

    def unsyncDags(project: Project, dags:Seq[DagName]): Task[Unit]
  }

  val dummy: ULayer[Git] = ZLayer.succeed[Service]( new Service {
    override def syncDags(project: Project, dags: Map[DagName, DagPayload]): Task[Unit] = Task.unit

    override def unsyncDags(project: Project, dags: Seq[DagName]): Task[Unit] = Task.unit
  })

  val live: ZLayer[Logging with Config, Nothing, Git] =  ZLayer.fromEffect (
    for {
      logging <- ZIO.access[Logging](_.get)
      gitConfig <- ZIO.access[Config](_.get)
    } yield new GitLive(gitConfig, logging)
  )


} 