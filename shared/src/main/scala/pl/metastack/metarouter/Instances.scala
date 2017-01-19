package pl.metastack.metarouter

import cats._
import cats.instances.all._
import cats.syntax.eq._

import shapeless.{HList, HNil, ::}

trait Instances {
  implicit object RouteEq extends Eq[Route[HList]] {
    override def eqv(x: Route[HList], y: Route[HList]): Boolean = {
      def cmp(l: HList, r: HList): Boolean =
        (l, r) match {
          case (HNil, HNil) => true
          case (_   , HNil) => false
          case (HNil, _   ) => false
          case ((aH: Arg[_]) :: aT, (bH: Arg[_]) :: bT) if aH.parseableArg == bH.parseableArg => cmp(aT, bT)
          case ((aH: Arg[_]) :: aT, (bH: Arg[_]) :: bT) if aH.parseableArg != bH.parseableArg => false
          case (aH :: aT, bH :: bT) if aH == bH => cmp(aT, bT)
          case (aH :: aT, bH :: bT) => false
        }

      cmp(x.pathElements, y.pathElements)
    }
  }
}
