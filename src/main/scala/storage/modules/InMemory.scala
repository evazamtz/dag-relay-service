package storage

import domain.{Dag, GitRepoSettings, Project}
import zio.{Ref, Task, ZLayer}

package object modules {

  class InMemory(projects: Ref[Map[String, Project]]) extends storage.Service {
    override def getProjects: Task[Map[String, Project]] = projects.get
  }

  val inMemory: ZLayer[Any, Nothing, Storage] = ZLayer.fromEffect(
    for {
      projects <- Ref.make(Map[String, Project]("core" -> Project("core", "jajasijdasjdaksjkakaka", "https://api.pimpay.ru/datamesh/dags", GitRepoSettings())))
    } yield (new InMemory(projects)).asInstanceOf[storage.Service]
  )
}