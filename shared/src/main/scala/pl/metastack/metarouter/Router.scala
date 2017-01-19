package pl.metastack.metarouter

import shapeless._
import shapeless.ops.hlist._

object Router {
  private[metarouter] class MappedRouterHelper[T] {
    def apply[ROUTE <: HList, Params <: HList](route: Route[ROUTE])
                                              (implicit p: Generic.Aux[T, Params],
                                                       ev: FlatMapper.Aux[Route.ConvertArgs.type, ROUTE, Params]
                                              ): MappedRoute[ROUTE, T] =
      new MappedRoute[ROUTE, T](route)
  }

  def route[T] = new MappedRouterHelper[T]

  def fill[ROUTE <: HList](route: Route[ROUTE])
                          (implicit ev: FlatMapper.Aux[Route.ConvertArgs.type, ROUTE, HNil]):
    RouteData[ROUTE, HNil] =
      RouteData[ROUTE, HNil](route, HNil)

  def fill[ROUTE <: HList, Args <: HList](route: Route[ROUTE], args: Args)
                                         (implicit ev: FlatMapper.Aux[Route.ConvertArgs.type, ROUTE, Args]):
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
                                        (implicit gen: FlatMapper.Aux[Route.ConvertArgs.type, ROUTE, Args]): String =
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