package pl.metastack.metarouter

import cats.Monoid
import cats.syntax.all._
import cats.instances.list._

import shapeless._
import shapeless.ops.hlist._
import shapeless.poly._

object Router {
  def parse[ROUTE <: HList, Args <: HList](route: Route[ROUTE], uri: String)
    (implicit ev: FlatMapper.Aux[Args.Convert.type, ROUTE, Args]): Option[Args] = {
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

    // Using asInstanceOf as a band aid since compiler isn't able to confirm the type.
    m(route.pathElements, PathParser.parseParts(uri)).map(_.asInstanceOf[Args])
  }

  def parseQuery[Params <: HList, Args <: HList](route: Params, query: String)
                                          (implicit ev: FlatMapper.Aux[Params.Convert.type, Params, Args]): Option[Args] = {
    def m[R <: HList](r: R, s: List[(String, String)]): Option[HList] =
      r match {
        case HNil => Some(HNil)
        case (ph: Param[_]) :: pt =>
          for {
            result <- s.find(_._1 == ph.name)
            decode <- ph.parseableArg.urlDecode(result._2)
            tail   <- m(pt, s.diff(List(result)))
          } yield decode :: tail
        case (ph: ParamOpt[_]) :: pt =>
          s.find(_._1 == ph.name) match {
            case None => m(pt, s).map(None :: _)
            case Some(result) =>
              val acc   = m(pt, s.diff(List(result)))
              val value = ph.parseableArg.urlDecode(result._2)
              acc.map(value :: _)
          }
      }

    val args = PathParser.parseArgs(query)
    m(route, args).map(_.asInstanceOf[Args])
  }

  def parse[ROUTE <: HList, Params <: HList, Args <: HList, ArgParams <: HList]
    (paramRoute: ParamRoute[ROUTE, Params], uri: String)
    (implicit ev: FlatMapper.Aux[Args.Convert.type, ROUTE, Args],
             ev2: FlatMapper.Aux[Params.Convert.type, Params, ArgParams]
    ): Option[(Args, ArgParams)] =
  {
    val params = uri.split('?')

    for {
      parts <- parse(paramRoute.route, params.head)
      args  <- parseQuery(paramRoute.params, params.tail.headOption.getOrElse(""))
    } yield (parts, args)
  }

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

  def url[ROUTE <: HList, Args <: HList](route: Route[ROUTE], args: Args)
                                        (implicit ev: FlatMapper.Aux[Args.Convert.type, ROUTE, Args]): String = {
    def build[R <: HList, A <: HList](r: R, a: A)(sb: String): String =
      (r, a) match {
        case (HNil, HNil) if sb.isEmpty => "/"
        case (HNil, HNil) => sb
        case ((h: String) :: t, _) => build(t, a)(s"$sb/$h")
        case ((a: Arg[_]) :: t, ah :: at) => build(t, at)(s"$sb/${a.asInstanceOf[Arg[Any]].parseableArg.urlEncode(ah)}")
      }

    build[ROUTE, Args](route.pathElements, args)("")
  }

  def url[ROUTE <: HList, Params <: HList, Args <: HList, ArgParams <: HList]
    (paramRoute: ParamRoute[ROUTE, Params], args: Args, params: ArgParams)
    (implicit ev: FlatMapper.Aux[Args.Convert.type, ROUTE, Args],
             ev2: FlatMapper.Aux[Params.Convert.type, Params, ArgParams]
    ): String =
  {
    def compose(acc: String, arg: ParseableArg[Any], name: String, value: Any): String = {
      val ampersand = if (acc.isEmpty) "" else s"$acc&"
      val encoded   = arg.urlEncode(value)
      ampersand + name + "=" + URI.encode(encoded)
    }

    def build[R <: HList, A <: HList](r: R, a: A)(sb: String): String =
      (r, a) match {
        case ((ph: ParamOpt[_]) :: pt, Some(vh) :: vt) =>
          build(pt, vt)(compose(sb, ph.asInstanceOf[ParamOpt[Any]].parseableArg,
            ph.name, vh))
        case ((ph: Param[_]) :: pt, vh :: vt) =>
          build(pt, vt)(compose(sb, ph.asInstanceOf[Param[Any]].parseableArg,
            ph.name, vh))
        case _ => sb
      }

    val base = url(paramRoute.route, args)

    build[Params, ArgParams](paramRoute.params, params)("") match {
      case "" => base
      case a  => base + "?" + a
    }
  }

  // Workaround for https://github.com/MetaStack-pl/MetaRouter/issues/18
  def url[ROUTE <: HList](route: Route[ROUTE], args: HNil.type)(
    implicit ev: FlatMapper.Aux[Args.Convert.type, ROUTE, HNil]
  ): String = url(route, args: HNil)

  def url[ROUTE <: HList, Params <: HList, Args <: HList, ArgParams <: HList]
    (paramRoute: ParamRoute[ROUTE, Params], args: HNil.type, params: ArgParams)
    (implicit ev: FlatMapper.Aux[Args.Convert.type, ROUTE, HNil],
             ev2: FlatMapper.Aux[Params.Convert.type, Params, ArgParams]
    ): String = url(paramRoute, args: HNil, params)
}