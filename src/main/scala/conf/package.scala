import zio.{Has, UIO}

package object conf {

  case class AppConf()
  case class LogConf()
  case class StorageConf()

  type Conf = Has[Service]

  trait Service {
    def load: UIO[AppConf]
  }
}

// CRUD???