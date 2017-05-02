package pl.metastack.metarouter

import shapeless._

case class Arg_[T](codec: Codec[T])

object Arg {
  def apply[T](implicit codec: Codec[T]): Arg_[T] = Arg_[T](codec)
}

object Args {
  object Convert extends Poly1 {
    implicit def caseString = at[String](_ => HNil)
    implicit def caseArg[T] = at[Arg[T]](_ => null: T :: HNil)
  }
}
