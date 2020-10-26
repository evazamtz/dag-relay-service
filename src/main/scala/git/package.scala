import domain.{Dag, GitRepoSettings}
import zio.{Has,Task}

package object git {
  type Git = Has[Service]

  trait Service {
    def syncDag(dag:Dag, gitRepoSettings: GitRepoSettings):Task[Unit]
  }
}