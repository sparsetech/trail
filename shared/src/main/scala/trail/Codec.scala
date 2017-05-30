package trail

import scala.util.Try

trait Codec[T] {
  def encode(s: T): String
  def decode(s: String): Option[T]
}

object Codec {
  implicit case object BooleanArg extends Codec[Boolean] {
    override def encode(s: Boolean): String = s.toString
    override def decode(s: String): Option[Boolean] = Try(s.toBoolean).toOption
  }

  implicit case object IntArg extends Codec[Int] {
    override def encode(s: Int): String = s.toString
    override def decode(s: String): Option[Int] = Try(s.toInt).toOption
  }

  implicit case object LongArg extends Codec[Long] {
    override def encode(s: Long): String = s.toString
    override def decode(s: String): Option[Long] = Try(s.toLong).toOption
  }

  implicit case object StringArg extends Codec[String] {
    override def encode(s: String): String = s
    override def decode(s: String): Option[String] = Option(s)
  }
}

