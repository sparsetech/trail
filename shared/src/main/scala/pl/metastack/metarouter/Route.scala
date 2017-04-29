package pl.metastack.metarouter

import shapeless._
import shapeless.ops.hlist._

object Route {
  val Root = Route[HNil](HNil)
}

case class Route[ROUTE <: HList](pathElements: ROUTE) {
  def /[T, E](a: T)(implicit pe: PathElement.Aux[T, E], prepend: Prepend[ROUTE, E :: HNil]) =
    Route(pathElements :+ pe.toPathElement(a))

  def &[T](param: Param[T]): ParamRoute[ROUTE, Param[T] :: HNil] =
    ParamRoute(this, param :: HNil)

  def &[T](param: ParamOpt[T]): ParamRoute[ROUTE, ParamOpt[T] :: HNil] =
    ParamRoute(this, param :: HNil)

  def unapply[Args <: HList](uri: String)
    (implicit ev: FlatMapper.Aux[Args.Convert.type, ROUTE, Args]): Option[Args] =
      Router.parse(this, uri)
}

case class ParamRoute[ROUTE <: HList, Params <: HList](route: Route[ROUTE], params: Params) {
  def &[T](param: Param[T])(implicit prepend: Prepend[Params, Param[T] :: HNil]) =
    copy(params = params :+ param)

  def &[T](param: ParamOpt[T])(implicit prepend: Prepend[Params, ParamOpt[T] :: HNil]) =
    copy(params = params :+ param)

  def unapply[Args <: HList, ArgParams <: HList](uri: String)
    (implicit ev: FlatMapper.Aux[Args.Convert.type, ROUTE, Args],
             ev2: FlatMapper.Aux[Params.Convert.type, Params, ArgParams]
    ): Option[(Args, ArgParams)] = Router.parse(this, uri)
}
