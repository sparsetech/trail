package pl.metastack.metarouter

import cats.Monoid
import cats.syntax.all._
import cats.instances.list._

import shapeless._
import shapeless.ops.hlist._
import shapeless.poly._

object Route {
  val Root = Route[HNil](HNil)

  /**
    * Converts all the chunks of the path to `HR` using the passed `~>>` function.
    * Then combines all the `HR` elements together.
    */
  def fold[
      ROUTE <: HList
    , H, HR: Monoid            // Head and Head Result - convert from what and to what
    , T <: HList, TR <: HList  // Tail and tail result
    , TLen <: Nat              // Length of the tail
  ](r: Route[ROUTE], f: Id ~>> HR)(implicit   // Infers ROUTE and HR
      cons: IsHCons.Aux[ROUTE, H, T]              // Infers H and T
    , tlen: Length .Aux[T, TLen]                  // Infers TLen. Length.Aux[T, Nothing] <: Length[T], so the implicit will be found and TLen will be set to its #Out
    , tr  : Fill   .Aux[TLen, HR, TR]             // Infers TR

    , hc  : Case1 .Aux[f.type, H, HR]              // Maps head
    , mt  : Mapper.Aux[f.type, ROUTE, HR :: TR]   // Maps tail
    , trav: ToTraversable.Aux[HR :: TR, List, HR]  // Converts HList to List
  ): HR =
    r.pathElements.map(f).toList[HR].combineAll
}

case class Route[ROUTE <: HList] private (pathElements: ROUTE) {
  def /[T, E](a: T)(implicit pe: PathElement.Aux[T, E], prepend: Prepend[ROUTE, E :: HNil]) =
    Route(pathElements :+ pe.toPathElement(a))
}

case class RouteData[ROUTE <: HList, DATA <: HList] private[metarouter](route: Route[ROUTE], data: DATA)

case class MappedRoute[ROUTE <: HList, T](route: Route[ROUTE])
