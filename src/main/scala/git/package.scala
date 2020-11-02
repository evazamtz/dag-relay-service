import domain.{Dag, GitRepoSettings}
import zio.{Has, Task, ULayer, ZLayer}

package object git {
  type Git = Has[Service]

  trait Service {
    def syncDag(dag:Dag, gitRepoSettings: GitRepoSettings):Task[Unit]
  }

  val dummy: ULayer[Git] = ZLayer.succeed(new Service {
    override def syncDag(dag: Dag, gitRepoSettings: GitRepoSettings): Task[Unit] = Task.unit
  })
}