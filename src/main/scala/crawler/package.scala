import domain._
import zio.{Has, Task}

package object crawler {
  type Crawler = Has[Service]

  trait Service {
    def fetch(project:Project):Task[Map[DagName, DagPayload]]
  }
}
