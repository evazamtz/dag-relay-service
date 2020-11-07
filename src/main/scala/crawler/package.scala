import crawler.modules.CrawlerLive
import domain._
import zio.{Has, Task, ULayer, ZLayer}

package object crawler {
  type Crawler = Has[Service]

  trait Service {
    def fetch(project:Project):Task[Map[DagName, DagPayload]]
  }

  val dummy: ULayer[Crawler] = ZLayer.succeed(_ => Task{ Map[DagName,DagPayload]()} )

  val live: ZLayer[Any, Nothing, Crawler] = ZLayer.succeed(new CrawlerLive)
}
