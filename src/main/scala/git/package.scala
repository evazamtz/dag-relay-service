import crawler.Crawler
import domain._
import git.modules.GitLive
import zio.{Task, _}



package object git {
  type Git = Has[Service]

  trait Service {
    def syncDags(project: Project, dags:Map[DagName,DagPayload]): Task[Unit]
    def syncProject(project: Project): RIO[Crawler, Map[DagName,DagPayload]]
  }

  val dummy: ULayer[Git] = ZLayer.succeed[Service](new Service {
    override def syncDags(project: Project, dags: Map[DagName, DagPayload]): Task[Unit] = Task.unit
    override def syncProject(project: Project): Task[Map[DagName,DagPayload]] = Task { Map.empty }
  })

  val live: ZLayer[Any, Nothing, Git] = ZLayer.succeed(new GitLive)
} 