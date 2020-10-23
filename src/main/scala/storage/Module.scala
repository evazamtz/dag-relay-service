import zio.Has
import zio.ZIO
import domain._

package object storage {

  case class StorageException() extends Throwable

  type StorageTask[+A] = ZIO[Any, StorageException, A]

  type Storage = Has[Module.Service]
  object Module {

    trait Service {
      def getProjects:StorageTask[Seq[Project]]
      def getResourcesByProject(project:String):StorageTask[Seq[Resource]]
      def getResourceByName(name:String):StorageTask[Resource]
      def getAllResources():StorageTask[Seq[Resource]]

      // TODO: updates/history
    }
  }
}