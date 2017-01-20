package pl.metastack.metarouter

import cats.Monoid
import cats.syntax.all._
import cats.instances.list._

import shapeless._
import shapeless.ops.hlist._
import shapeless.poly._

class Router(parsers: List[String => Option[Any]]) {
  def orElse[ROUTE <: HList, T, L <: HList](other: MappedRoute[ROUTE, T])
                                           (implicit gen: Generic.Aux[T, L],
                                            map: FlatMapper.Aux[Args.Convert.type, ROUTE, L]) = {
    val f: String => Option[Any] = Router.parse(other, _)
    new Router(parsers :+ f)
  }

  def parse(uri: String): Option[Any] =
    parsers.foldLeft(Option.empty[Any]) { case (acc, cur) =>
      acc.orElse(cur(uri))
    }
}

object Router {
  private[metarouter] class MappedRouterHelper[T] {
    def apply[ROUTE <: HList, Params <: HList](route: Route[ROUTE])
                                              (implicit p: Generic.Aux[T, Params],
                                                       ev: FlatMapper.Aux[Args.Convert.type, ROUTE, Params]
                                              ): MappedRoute[ROUTE, T] =
      new MappedRoute[ROUTE, T](route)
  }

  def route[T] = new MappedRouterHelper[T]

  private[metarouter] def split(s: String): List[String] = {
    val x = s.stripPrefix("/")
    if (x.isEmpty) Nil
    else x.split('/').toList
  }

  def create[ROUTE <: HList, T, L <: HList](route: MappedRoute[ROUTE, T])
                                           (implicit gen: Generic.Aux[T, L],
                                                     map: FlatMapper.Aux[Args.Convert.type, ROUTE, L]) =
    new Router(List(Router.parse(route, _)))

  // TODO Figure out what to do with relative routes and query parameters
  def parse(s: String): RouteData[HList, HNil] = {
    val r = Route(split(s).foldRight[HList](HNil)((x, y) => x :: y))
    RouteData(r, HNil)
  }

  def parse[ROUTE <: HList, L <: HList](route: Route[ROUTE], s: String)
    (implicit map: FlatMapper.Aux[Args.Convert.type, ROUTE, L]):
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

    m[ROUTE](route.pathElements, split(s)).map { x =>
      // Using asInstanceOf as a band aid since compiler isn't able to confirm the type.
      RouteData(route, x.asInstanceOf[L])
    }
  }

  def parse[ROUTE <: HList, T, Args <: HList](mappedRoute: MappedRoute[ROUTE, T], uri: String)
                                             (implicit gen: Generic.Aux[T, Args],
                                                       map: FlatMapper.Aux[Args.Convert.type, ROUTE, Args]): Option[T] =
    parse(mappedRoute.route, uri).map(parsed => gen.from(parsed.data))

  /**
    * Converts all the chunks of the path to `HR` using the passed `~>>` function.
    * Then combines all the `HR` elements together.
    */
  // TODO Can be done better with `foldMap`
  def fold[
      ROUTE <: HList
    , H, HR: Monoid            // Head and head result - convert from what and to what
    , T <: HList, TR <: HList  // Tail and tail result
    , TLen <: Nat              // Length of the tail
  ](r: Route[ROUTE], f: Id ~>> HR)(implicit        // Infers ROUTE and HR
      cons: IsHCons.Aux[ROUTE, H, T]               // Infers H and T
    , tlen: Length .Aux[T, TLen]                   // Infers TLen. Length.Aux[T, Nothing] <: Length[T], so the implicit will be found and TLen will be set to its #Out
    , tr  : Fill   .Aux[TLen, HR, TR]              // Infers TR

    , hc  : Case1 .Aux[f.type, H, HR]              // Maps head
    , mt  : Mapper.Aux[f.type, ROUTE, HR :: TR]    // Maps tail
    , trav: ToTraversable.Aux[HR :: TR, List, HR]  // Converts HList to List
  ): HR =
    r.pathElements.map(f).toList[HR].combineAll

  def fill[ROUTE <: HList](route: Route[ROUTE])
                          (implicit ev: FlatMapper.Aux[Args.Convert.type, ROUTE, HNil]):
    RouteData[ROUTE, HNil] =
      RouteData[ROUTE, HNil](route, HNil)

  def fill[ROUTE <: HList, Args <: HList](route: Route[ROUTE], args: Args)
                                         (implicit ev: FlatMapper.Aux[Args.Convert.type, ROUTE, Args]):
    RouteData[ROUTE, Args] =
      RouteData[ROUTE, Args](route, args)

  def fill[ROUTE <: HList, T, Args <: HList](mapped: MappedRoute[ROUTE, T], data: T)
                                            (implicit gen: Generic.Aux[T, Args]):
    RouteData[ROUTE, Args] =
      RouteData[ROUTE, Args](mapped.route, gen.to(data))

  def fill[ROUTE <: HList, T, Args <: HList](data: T)
                                            (implicit gen: Generic.Aux[T, Args],
                                                   mapped: MappedRoute[ROUTE, T]):
    RouteData[ROUTE, Args] = fill(mapped, data)

  def url[ROUTE <: HList, DATA <: HList](routeData: RouteData[ROUTE, DATA]): String = {
    def build[R <: HList, A <: HList](r: R, a: A)(sb: String): String =
      (r, a) match {
        case (HNil, HNil) if sb.isEmpty => "/"
        case (HNil, HNil) => sb
        case ((h: String) :: t, _) => build(t, a)(s"$sb/$h")
        case ((a: Arg[_]) :: t, ah :: at) => build(t, at)(s"$sb/${a.asInstanceOf[Arg[Any]].parseableArg.urlEncode(ah)}")
      }

    build[ROUTE, DATA](routeData.route.pathElements, routeData.data)("")
  }

  def url[ROUTE <: HList, Args <: HList](route: Route[ROUTE], args: Args)
                                        (implicit gen: FlatMapper.Aux[Args.Convert.type, ROUTE, Args]): String =
    url(RouteData(route, args))

  def url[ROUTE <: HList, T](mapped: MappedRoute[ROUTE, T], data: T)
                            (implicit gen: Generic[T]): String = {
    val args = gen.to(data).asInstanceOf[HList]
    url(RouteData(mapped.route, args))
  }

  def url[T, ROUTE <: HList](data: T)(implicit gen: Generic[T],
                                            mapped: MappedRoute[ROUTE, T]
                                     ): String = url(mapped, data)
}