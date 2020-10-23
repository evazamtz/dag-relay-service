import zio.{Has, UIO}

package object log {
  type Log = Has[log.Service]

  trait Service {
    def log(level: String, msg: String, tags: Seq[String]):UIO[Unit]
    // TODO: tags/markers? map + seq? correlationId, scope?!!?!!
    // tobo: make a better algebra
  }
}
