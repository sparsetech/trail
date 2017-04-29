package pl.metastack.metarouter

import shapeless._

import scala.util.Try

case class Arg_     [T](parseableArg: ParseableArg[T])
case class Param_   [T](name: String, parseableArg: ParseableArg[T])
case class ParamOpt_[T](name: String, parseableArg: ParseableArg[T])

object Arg {
  def apply[T](implicit parseableArg: ParseableArg[T]): Arg_[T] =
    Arg_[T](parseableArg)
}

object Param {
  def apply[T](name: String)
              (implicit parseableArg: ParseableArg[T]): Param_[T] =
    Param_[T](name, parseableArg)
}

object ParamOpt {
  def apply[T](name: String)
              (implicit parseableArg: ParseableArg[T]): ParamOpt_[T] =
    ParamOpt_[T](name, parseableArg)
}

trait ParseableArg[T] {
  def urlDecode(s: String): Option[T]
  def urlEncode(s: T): String
}

object ParseableArg {
  implicit case object BooleanArg extends ParseableArg[Boolean] {
    override def urlDecode(s: String): Option[Boolean] = Try(s.toBoolean).toOption
    override def urlEncode(s: Boolean): String = s.toString
  }

  implicit case object IntArg extends ParseableArg[Int] {
    override def urlDecode(s: String): Option[Int] = Try(s.toInt).toOption
    override def urlEncode(s: Int): String = s.toString
  }

  implicit case object LongArg extends ParseableArg[Long] {
    override def urlDecode(s: String): Option[Long] = Try(s.toLong).toOption
    override def urlEncode(s: Long): String = s.toString
  }

  implicit case object StringArg extends ParseableArg[String] {
    override def urlDecode(s: String): Option[String] = Option(s)
    override def urlEncode(s: String): String = s
  }
}

object Args {
  object Convert extends Poly1 {
    implicit def default    = at[String](_ => HNil)
    implicit def caseArg[T] = at[Arg[T]](_ => null.asInstanceOf[T] :: HNil)
  }
}

object Params {
  object Convert extends Poly1 {
    implicit def caseParam   [T] = at[Param   [T]](_ => null.asInstanceOf[T        ] :: HNil)
    implicit def caseParamOpt[T] = at[ParamOpt[T]](_ => null.asInstanceOf[Option[T]] :: HNil)
  }
}
