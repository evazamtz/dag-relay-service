package storage

import domain.{Dag, GitRepoSettings, Project}
import zio.{Ref, Task, ZLayer}

import scala.collection.mutable.{Map => MMap}

package object modules {

  class InMemory(projects: Ref[MMap[String, Project]]) extends storage.Service {
    override def getProjects: Task[Seq[Project]] = projects.get map { p => p.values.toSeq }
  }

  val inMemory: ZLayer[Any, Nothing, Storage] = ZLayer.fromEffect(
    for {
     projects <- Ref.make(MMap[String, Project]("core" -> Project("core", "jahsdjksasd", "https://api.pimpay.ru/datamesh/dags", GitRepoSettings("", "", "", ""))))
    } yield (new InMemory(projects)).asInstanceOf[storage.Service]
  )
}