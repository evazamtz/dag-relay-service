import domain._
import zio.{Has, Task, ULayer, ZLayer}
import crawler.modules.HttpCrawler

package object crawler {
  type Crawler = Has[Service]

  trait Service {
    def fetch(project:Project):Task[Map[DagName, DagPayload]]
  }

  val dummy: ULayer[Crawler] = ZLayer.succeed(_ => Task{ Map[DagName,DagPayload]()} )

  val live: ULayer[Crawler] = ZLayer.succeed(new HttpCrawler)
}
