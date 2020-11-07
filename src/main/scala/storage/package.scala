import zio.{Has, Task}
import domain._

package object storage {

  type Storage = Has[Service]

  trait Service {
    def getProjects:Task[Map[ProjectName, Project]]
  }
}