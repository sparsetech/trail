package pl.metastack.metarouter

import cats.Monoid
import cats.syntax.all._
import cats.instances.list._

import shapeless._
import shapeless.ops.hlist._
import shapeless.poly._

object Route {
  // TODO Figure out what to do with relative routes and query parameters
  def parse(s: String) = {
    val r = Route(split(s).foldRight[HList](HNil)((x, y) => x :: y))
    RouteData(r, HNil)
  }

  def split(s: String): List[String] = {
    val x = s.stripPrefix("/")
    if (x.isEmpty) Nil
    else x.split('/').toList
  }

  trait Drop extends Poly1 {
    implicit def default[T] = at[T](x => HNil)
  }
  object ConvertArgs extends Drop {
    implicit def caseArg[T] = at[Arg[T]].apply[T :: HNil](x => null.asInstanceOf[T] :: HNil)
  }

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
  ](r: RouteBase[ROUTE], f: Id ~>> HR)(implicit   // Infers ROUTE and HR
      cons: IsHCons.Aux[ROUTE, H, T]              // Infers H and T
    , tlen: Length .Aux[T, TLen]                  // Infers TLen. Length.Aux[T, Nothing] <: Length[T], so the implicit will be found and TLen will be set to its #Out
    , tr  : Fill   .Aux[TLen, HR, TR]             // Infers TR

    , hc  : Case1 .Aux[f.type, H, HR]              // Maps head
    , mt  : Mapper.Aux[f.type, ROUTE, HR :: TR]   // Maps tail
    , trav: ToTraversable.Aux[HR :: TR, List, HR]  // Converts HList to List
  ): HR =
    r.path.map(f).toList[HR].combineAll
}

trait RouteBase[ROUTE <: HList] {
  def path: ROUTE

  def fold[
    H, HR: Monoid
  , T <: HList, TR <: HList
  , TLen <: Nat
  ](f: Id ~>> HR)(implicit
      cons: IsHCons.Aux[ROUTE, H, T]
    , tlen: Length .Aux[T, TLen]
    , tr  : Fill   .Aux[TLen, HR, TR]

    , hc  : Case1  .Aux[f.type, H, HR]
    , mt  : Mapper .Aux[f.type, ROUTE, HR :: TR]
    , trav: ToTraversable.Aux[HR :: TR, List, HR]
  ): HR = Route.fold(this, f)
}

case class Route[ROUTE <: HList] private (pathElements: ROUTE) extends RouteBase[ROUTE] {
  def path = pathElements

  def parse[L <: HList](s: String)
    (implicit map: FlatMapper.Aux[Route.ConvertArgs.type, ROUTE, L]):
    Option[RouteData[ROUTE, L]] =
  {
    import shapeless.HList.ListCompat._

    def m[R <: HList](r: R, s: Seq[String]): Option[HList] =
      (r, s) match {
        case (HNil, Nil) => Some(HNil)
        case (_,    Nil) => None
        case (HNil, _)   => None
        case ((rH: String)  #: rT, sH :: sT) if rH == sH => m(rT, sT)
        case ((rH: String)  #: rT, sH :: sT) => None
        case ((arg: Arg[_]) #: rT, sH :: sT) =>
          arg.parseableArg.urlDecode(sH).flatMap { decoded =>
            m(rT, sT).map(decoded :: _)
          }
      }

    m[ROUTE](pathElements, Route.split(s)).map { x =>
      // Using asInstanceOf as a band aid since compiler isn't able to confirm the type.
      RouteData(this, x.asInstanceOf[L])
    }
  }

  def /[T, E](a: T)(implicit pe: PathElement.Aux[T, E], prepend: Prepend[ROUTE, E :: HNil]) =
    Route(pathElements :+ pe.toPathElement(a))
}

case class RouteData[ROUTE <: HList, DATA <: HList] private[metarouter](route: Route[ROUTE], data: DATA) extends RouteBase[ROUTE] {
  override def path: ROUTE = route.path
}

case class MappedRoute[ROUTE <: HList, T](route: Route[ROUTE]) extends RouteBase[ROUTE] {
  override def path: ROUTE = route.path

  def parse[L <: HList](uri: String)
                       (implicit gen: Generic.Aux[T, L],
                        map: FlatMapper.Aux[Route.ConvertArgs.type, ROUTE, L]): Option[T] =
    route.parse(uri).map(parsed => gen.from(parsed.data))
}

class ComposedRoute(parsers: Seq[(String => Option[Any])]) {
  def orElse[ROUTE <: HList, T, L <: HList](other: MappedRoute[ROUTE, T])
                                           (implicit gen: Generic.Aux[T, L],
                                            map: FlatMapper.Aux[Route.ConvertArgs.type, ROUTE, L]) = {
    val f: String => Option[Any] = other.parse(_)
    new ComposedRoute(parsers :+ f)
  }

  def parse(uri: String): Option[Any] =
    parsers.foldLeft(Option.empty[Any]) { case (acc, cur) =>
      acc.orElse(cur(uri))
    }
}

object ComposedRoute {
  def apply[ROUTE <: HList, T, L <: HList](route: MappedRoute[ROUTE, T])
                                          (implicit gen: Generic.Aux[T, L],
                                           map: FlatMapper.Aux[Route.ConvertArgs.type, ROUTE, L]) =
    new ComposedRoute(Seq(route.parse(_)))
}
