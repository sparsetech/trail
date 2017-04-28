package pl.metastack.metarouter

import shapeless._
import shapeless.ops.hlist._

object Route {
  val Root = Route[HNil](HNil)
}

case class Route[ROUTE <: HList](pathElements: ROUTE) {
  def /[T, E](a: T)(implicit pe: PathElement.Aux[T, E], prepend: Prepend[ROUTE, E :: HNil]) =
    Route(pathElements :+ pe.toPathElement(a))

  def unapply[Args <: HList](uri: String)(implicit map: FlatMapper.Aux[Args.Convert.type, ROUTE, Args]): Option[Args] =
    Router.parse(this, uri)
}
