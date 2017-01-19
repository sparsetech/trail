package pl.metastack.metarouter

import shapeless._
import shapeless.ops.hlist._

object Route {
  val Root = Route[HNil](HNil)
}

case class Route[ROUTE <: HList] private[metarouter] (pathElements: ROUTE) {
  def /[T, E](a: T)(implicit pe: PathElement.Aux[T, E], prepend: Prepend[ROUTE, E :: HNil]) =
    Route(pathElements :+ pe.toPathElement(a))
}

case class RouteData[ROUTE <: HList, DATA <: HList] private[metarouter] (route: Route[ROUTE], data: DATA)

case class MappedRoute[ROUTE <: HList, T] private[metarouter] (route: Route[ROUTE])