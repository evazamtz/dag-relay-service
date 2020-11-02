import zio.ZIO

package object utils {

  implicit class OptionSyntax[A](v:Option[A]) {
    def getOrFail[E](e:E):ZIO[Any, E, A] = ZIO.fromOption(v).orElseFail(e)
  }

  def ensure[E](cond: Boolean)(e:E):ZIO[Any, E, Unit] = if (cond) ZIO.unit else ZIO.fail(e)

  implicit class ZIOSyntax(z:ZIO.type) {
    def ensureOrFail[E](predicate:Boolean, e: => E):ZIO[Any, E, Unit] = if (predicate) ZIO.unit else ZIO.fail(e)
  }
}
