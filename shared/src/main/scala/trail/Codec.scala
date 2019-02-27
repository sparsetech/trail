package trail

import scala.util.Try

trait Codec[T] {
  def encode(value: T): Option[String]
  def decode(value: Option[String]): Option[T]
}

object Codec {
  implicit case object BooleanCodec extends Codec[Boolean] {
    override def encode(s: Boolean): Option[String] = Some(s.toString)
    override def decode(s: Option[String]): Option[Boolean] =
      s.flatMap(s => Try(s.toBoolean).toOption)
  }

  implicit case object IntCodec extends Codec[Int] {
    override def encode(s: Int): Option[String] = Some(s.toString)
    override def decode(s: Option[String]): Option[Int] =
      s.flatMap(s => Try(s.toInt).toOption)
  }

  implicit case object LongCodec extends Codec[Long] {
    override def encode(s: Long): Option[String] = Some(s.toString)
    override def decode(s: Option[String]): Option[Long] =
      s.flatMap(s => Try(s.toLong).toOption)
  }

  implicit case object StringCodec extends Codec[String] {
    override def encode(s: String): Option[String] = Some(s)
    override def decode(s: Option[String]): Option[String] = s
  }

  implicit def OptionCodec[T](implicit codec: Codec[T]): Codec[Option[T]] =
    new Codec[Option[T]] {
      override def encode(s: Option[T]): Option[String] =
        s.flatMap(codec.encode)
      override def decode(s: Option[String]): Option[Option[T]] =
        if (s.isEmpty) Some(None) else codec.decode(s).map(Some(_))
    }
}
