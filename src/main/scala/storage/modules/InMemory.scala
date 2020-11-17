package storage

import domain._
import zio.{Task, ULayer, ZLayer}

package object modules {

  class InMemory(projects: Map[ProjectName, Project]) extends storage.Service {
    override def getProjects: Task[Map[ProjectName, Project]] = Task { projects }
  }

  val gitRepoSettings = GitRepoSettings(
    "https://gitlab.pimpay.ru/api/v4/projects/294/repository",
    "master",
    "dags",
    "RtPpsq7iiFv2xQiDdU8J"
  )

  val inMemory: ULayer[Storage] = ZLayer.succeed {
    new InMemory(Map[ProjectName, Project]("core" -> Project("core", "jajasijdasjdaksjkakaka", "https://api.pimpay.ru/datamesh/dags", gitRepoSettings))).asInstanceOf[storage.Service]
  }
}