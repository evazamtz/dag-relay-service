import domain._
import git.modules.GitLive
import zio.logging.{Logger, Logging}
import zio.{Task, _}



package object git {
  type Git = Has[Service]

  trait Service {
    def syncDags(project: Project, dags:Map[DagName,DagPayload]): Task[Unit]
  }

  val dummy: ULayer[Git] = ZLayer.succeed[Service]((project: Project, dags: Map[DagName, DagPayload]) => Task.unit)

  def live(parallelism: Int): ZLayer[Logging, Nothing, Git] =  ZLayer.fromEffect (
    for {
      logging <- ZIO.access[Logging](_.get)
    } yield new GitLive(parallelism, logging)
  )


} 