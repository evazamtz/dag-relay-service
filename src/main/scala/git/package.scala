import config._
import domain._
import git.modules.GitLive
import zio.logging._
import zio.{Task, _}
import config.{Service => ConfigService}



package object git {
  type Git = Has[Service]

  trait Service {
    def syncDags(project: Project, dags:Map[DagName,DagPayload]): Task[Unit]

    def desyncDags(project: Project, dags:Seq[DagName]): Task[Unit]

    def getNamesByProject(project: Project) : Task[Seq[DagName]]
  }

  val dummy: ULayer[Git] = ZLayer.succeed[Service]( new Service {
    override def syncDags(project: Project, dags: Map[DagName, DagPayload]): Task[Unit] = Task.unit

    override def desyncDags(project: Project, dags: Seq[DagName]): Task[Unit] = Task.unit

    override def getNamesByProject(project: Project): Task[Seq[DagName]] = Task(Seq.empty[DagName])
  })

  val liveFromConfig: ZLayer[Logging with Config, Nothing, Git] =  (for {
    logging <- ZIO.access[Logging](_.get)
    gitConfig <- ZIO.access[Config](_.get)
    appConf <- gitConfig.app
  } yield GitLive(appConf.git.parallelism, logging)).toLayer


  def live(parallelism: Int): ZLayer[Logging, Nothing, Git] =  ZLayer.fromService { logging => GitLive(parallelism, logging) }
} 