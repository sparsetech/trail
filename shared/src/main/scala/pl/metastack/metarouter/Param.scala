package pl.metastack.metarouter

import shapeless._

case class Param_   [T](name: String, codec: Codec[T])
case class ParamOpt_[T](name: String, codec: Codec[T])

object Param {
  def apply[T](name: String)(implicit codec: Codec[T]): Param_[T] =
    Param_[T](name, codec)
}

object ParamOpt {
  def apply[T](name: String)(implicit codec: Codec[T]): ParamOpt_[T] =
    ParamOpt_[T](name, codec)
}

object Params {
  object Convert extends Poly1 {
    implicit def caseParam   [T] = at[Param   [T]](_ => null: T         :: HNil)
    implicit def caseParamOpt[T] = at[ParamOpt[T]](_ => null: Option[T] :: HNil)
  }
}
