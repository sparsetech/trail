package pl.metastack.metarouter

import cats.Monoid
import cats.syntax.all._
import cats.instances.list._

import shapeless.PolyDefns.Case
import shapeless._
import shapeless.ops.hlist._
import shapeless.poly._
import shapeless.ops.hlist.Fill.Aux
import shapeless.ops.hlist.IsHCons.Aux
import shapeless.ops.hlist.Length.Aux
import shapeless.ops.hlist.Mapper.Aux
import shapeless.ops.hlist.ToTraversable.Aux

object Route {
  // TODO: Figure out what to do with relative routes.
  def parse(s: String) = {
    val r = Route(split(s).foldRight[HList](HNil)((x, y) => x :: y))
    InstantiatedRoute(r, HNil)
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
    Option[InstantiatedRoute[ROUTE, L]] =
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
      InstantiatedRoute(this, x.asInstanceOf[L])
    }
  }

  def /[T, E](a: T)(implicit pe: PathElement.Aux[T, E], prepend: Prepend[ROUTE, E :: HNil]) =
    Route(pathElements :+ pe.toPathElement(a))

  override def canEqual(other: Any): Boolean = other.isInstanceOf[Route[ROUTE]]

  override def equals(other: scala.Any): Boolean = {
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

    other match {
      case o: Route[ROUTE] => cmp(pathElements, o.pathElements)
      case _ => false
    }
  }

  override def hashCode(): Int = {
    def build[R <: HList](p: R): Int =
      p match {
        case HNil => 0
        case (h: Arg[_]) :: t => h.parseableArg.hashCode() ^ build(t)
        case h :: t => h.hashCode() ^ build(t)
      }

    build(pathElements)
  }
}

case class InstantiatedRoute[ROUTE <: HList, DATA <: HList] private[metarouter] (route: Route[ROUTE], data: DATA) extends RouteBase[ROUTE] {
  override def path: ROUTE = route.path

  def url(): String = {
    def build[R <: HList, A <: HList](r: R, a: A)(sb: String): String =
      (r, a) match {
        case (HNil, HNil) if sb.isEmpty => "/"
        case (HNil, HNil) => sb
        case ((h: String) :: t, _) => build(t, a)(s"$sb/$h")
        case ((a: Arg[_]) :: t, ah :: at) => build(t, at)(s"$sb/${a.asInstanceOf[Arg[Any]].parseableArg.urlEncode(ah)}")
      }

    build[ROUTE, DATA](route.pathElements, data)("")
  }

  override def canEqual(other: Any): Boolean = other.isInstanceOf[InstantiatedRoute[ROUTE, DATA]]

  override def equals(other: Any): Boolean =
    other match {
      case o: InstantiatedRoute[_, _] => url() == o.url()
      case _ => false
    }

  override def hashCode(): Int = url().hashCode
}

case class MappedRoute[ROUTE <: HList, T](route: Route[ROUTE]) extends RouteBase[ROUTE] {
  override def path: ROUTE = route.path

  def apply[L <: HList](value: T)(implicit gen: Generic.Aux[T, L]):
  InstantiatedRoute[ROUTE, L] =
    InstantiatedRoute[ROUTE, L](route, gen.to(value))

  def parse[L <: HList](uri: String)
                       (implicit gen: Generic.Aux[T, L],
                        map: FlatMapper.Aux[Route.ConvertArgs.type, ROUTE, L]): Option[T] =
    route.parse(uri).map(parsed => gen.from(parsed.data))
}

object Router {
  private[metarouter] class MappedRouterHelper[T] {
    def apply[ROUTE <: HList, Params <: HList](route: Route[ROUTE])
                                              (implicit p: Generic.Aux[T, Params],
                                                       ev: FlatMapper.Aux[Route.ConvertArgs.type, ROUTE, Params]
                                              ): MappedRoute[ROUTE, T] =
      new MappedRoute[ROUTE, T](route)
  }

  def route[T] = new MappedRouterHelper[T]

  def url[T, ROUTE <: HList](data: T)(implicit gen: Generic[T],
                                               mapped: MappedRoute[ROUTE, T]
                                     ): String = {
    val l = gen.to(data)
    InstantiatedRoute(mapped.route, l.asInstanceOf[HList]).url()
  }

  def fill[ROUTE <: HList](route: Route[ROUTE])
                          (implicit ev: FlatMapper.Aux[Route.ConvertArgs.type, ROUTE, HNil]):
    InstantiatedRoute[ROUTE, HNil] =
      InstantiatedRoute[ROUTE, HNil](route, HNil)

  def fill[ROUTE <: HList, Args <: HList](route: Route[ROUTE], args: Args)
                                         (implicit ev: FlatMapper.Aux[Route.ConvertArgs.type, ROUTE, Args]):
    InstantiatedRoute[ROUTE, Args] =
      InstantiatedRoute[ROUTE, Args](route, args)
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
