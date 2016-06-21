package pl.metastack.metarouter

import scala.util.Try

case class Arg[T](implicit val parseableArg: ParseableArg[T])

trait ParseableArg[T] {
  def urlDecode(s: String): Option[T]
  def urlEncode(s: T): String
}

object ParseableArg {
  implicit case object BooleanArg extends ParseableArg[Boolean] {
    override def urlDecode(s: String) = Try(s.toBoolean).toOption
    override def urlEncode(s: Boolean) = s.toString
  }

  implicit case object IntArg extends ParseableArg[Int] {
    override def urlDecode(s: String) = Try(s.toInt).toOption
    override def urlEncode(s: Int) = s.toString
  }

  implicit case object StringArg extends ParseableArg[String] {
    override def urlDecode(s: String) = Option(s)
    override def urlEncode(s: String) = s
  }
}
