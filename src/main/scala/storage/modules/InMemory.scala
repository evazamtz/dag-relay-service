package storage

import domain.{Dag, GitRepoSettings, Project}
import zio.{Ref, Task, ZLayer}

import scala.collection.mutable.{Map => MMap}

package object modules {

  class InMemory(projects: Ref[MMap[String, Project]], dags: Ref[MMap[(String, String), Dag]]) extends storage.Service {
    override def getProjects: Task[Seq[Project]] = projects.get map { p => p.values.toSeq }
    override def getDagsByProject(project: String): Task[Seq[Dag]] = dags.get map { dags => dags.values.filter(_.project == project).toSeq }
    override def findDag(project: String, name: String): Task[Option[Dag]] = dags.get map { dags => dags.get(project -> name) }
  }

  val inMemory: ZLayer[Any, Nothing, Storage] = ZLayer.fromEffect(
    for {
      projects <- Ref.make(MMap[String, Project]("core" -> Project("core", "jahsdjksasd", "https://api.pimpay.ru/datamesh/dags", GitRepoSettings())))
      dags     <- Ref.make(MMap[(String,String), Dag]())
    } yield (new InMemory(projects, dags)).asInstanceOf[storage.Service]
  )
}