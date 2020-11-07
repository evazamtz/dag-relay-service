import domain._
import zio.Task

package object crawler {
  trait Service {
    def fetch(project:Project):Task[Map[DagName, DagPayload]]
  }
}
