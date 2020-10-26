import zio.{Has, Task, ZLayer}
import domain._

package object storage {

  type Storage = Has[Service]

  trait Service {
    def getProjects:Task[Seq[Project]]
    def getDagsByProject(project:String):Task[Seq[Dag]]
    def findDag(project:String, name:String):Task[Option[Dag]]
  }
}