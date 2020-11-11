import zio._
import domain._

package object storage {

  type Storage = Has[Service]

  trait Service {
    def getProjects:Task[Map[ProjectName, Project]]
  }

  def getProjects: RIO[Storage, Map[ProjectName, Project]] = ZIO.accessM[Storage](_.get.getProjects)
}