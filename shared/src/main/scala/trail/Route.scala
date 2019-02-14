package trail

import cats.Monoid
import cats.syntax.all._
import cats.instances.list._

import shapeless._
import shapeless.ops.hlist._
import shapeless.poly._

object Route {
  val Root = Route[HNil](HNil)
}

case class Route[ROUTE <: HList](pathElements: ROUTE) {
  def apply()(
    implicit ev: FlatMapper.Aux[Args.Convert.type, ROUTE, HNil]
  ): String = url(HNil: HNil)

  def apply[Args <: HList](args: Args)(
    implicit ev: FlatMapper.Aux[Args.Convert.type, ROUTE, Args]
  ): String = url(args)

  // Workaround for https://github.com/MetaStack-pl/MetaRouter/issues/18
  def apply(args: HNil.type)(
    implicit ev: FlatMapper.Aux[Args.Convert.type, ROUTE, HNil]
  ): String = url(args: HNil)

  def unapply[Args <: HList](uri: String)(
    implicit ev: FlatMapper.Aux[Args.Convert.type, ROUTE, Args]
  ): Option[Args] = parse(uri)

  def url()(
    implicit ev: FlatMapper.Aux[Args.Convert.type, ROUTE, HNil]
  ): String = url(HNil: HNil)

  def url[Args <: HList](args: Args)(
    implicit ev: FlatMapper.Aux[Args.Convert.type, ROUTE, Args]
  ): String = {
    def build[R <: HList, A <: HList](r: R, a: A)(sb: String): String =
      (r, a) match {
        case (HNil, HNil) if sb.isEmpty => "/"
        case (HNil, HNil) => sb
        case ((h: String) :: t, _) => build(t, a)(s"$sb/$h")
        case ((a: Arg[_]) :: t, ah :: at) =>
          build(t, at)(s"$sb/${a.asInstanceOf[Arg[Any]].codec.encode(ah)}")
      }

    build[ROUTE, Args](pathElements, args)("")
  }

  def url(args: HNil.type)(
    implicit ev: FlatMapper.Aux[Args.Convert.type, ROUTE, HNil]
  ): String = url(args: HNil)

  def parse[Args <: HList](uri: String)(
    implicit ev: FlatMapper.Aux[Args.Convert.type, ROUTE, Args]
  ): Option[Args] = {
    import shapeless.HList.ListCompat._

    def m[R <: HList](r: R, s: Seq[String]): Option[HList] =
      (r, s) match {
        case (HNil, Nil) => Some(HNil)
        case (_,    Nil) => None
        case (HNil, _)   => None
        case ((rH: String)  #: rT, sH :: sT) if rH == sH => m(rT, sT)
        case ((rH: String)  #: rT, sH :: sT) => None
        case ((arg: Arg[_]) #: rT, sH :: sT) =>
          arg.codec.decode(sH).flatMap { decoded =>
            m(rT, sT).map(decoded :: _)
          }
      }

    // Using asInstanceOf as a band aid since compiler isn't able to confirm the
    // type.
    m(pathElements, PathParser.parseParts(uri)).map(_.asInstanceOf[Args])
  }

  /**
    * Converts all the chunks of the path to `HR` using the passed `~>>` function.
    * Then combines all the `HR` elements together.
    */
  // TODO Can be done better with `foldMap`
  def fold[
    H, HR: Monoid                                // Head and head result - convert from what and to what
  , T <: HList, TR <: HList                      // Tail and tail result
  , TLen <: Nat                                  // Length of the tail
  ](f: Id ~>> HR)(implicit                       // Infers ROUTE and HR
    cons: IsHCons.Aux[ROUTE, H, T]               // Infers H and T
  , tlen: Length .Aux[T, TLen]                   // Infers TLen. Length.Aux[T, Nothing] <: Length[T], so the implicit will be found and TLen will be set to its #Out
  , tr  : Fill   .Aux[TLen, HR, TR]              // Infers TR
  , hc  : Case1  .Aux[f.type, H, HR]             // Maps head
  , mt  : Mapper .Aux[f.type, ROUTE, HR :: TR]   // Maps tail
  , trav: ToTraversable.Aux[HR :: TR, List, HR]  // Converts HList to List
  ): HR =
    pathElements.map(f).toList[HR].combineAll

  def /[T, U](a: T)(
    implicit pe: PathElement[T, U], prepend: Prepend[ROUTE, U :: HNil]
  ) = Route(pathElements :+ pe.f(a))

  def &[T](param: Param[T]): ParamRoute[ROUTE, Param[T] :: HNil] =
    ParamRoute(this, param :: HNil)

  def &[T](param: ParamOpt[T]): ParamRoute[ROUTE, ParamOpt[T] :: HNil] =
    ParamRoute(this, param :: HNil)

  def &[T](param: Fragment[T]): ParamRoute[ROUTE, Fragment[T] :: HNil] =
    ParamRoute(this, param :: HNil)
}

case class ParamRoute[ROUTE <: HList, Params <: HList](route: Route[ROUTE], params: Params) {
  def apply[Args <: HList, ArgParams <: HList](args: Args, argParams: ArgParams)(
    implicit ev: FlatMapper.Aux[Args.Convert.type, ROUTE, Args],
            ev2: FlatMapper.Aux[Params.Convert.type, Params, ArgParams]
  ): String = url(args, argParams)

  def apply[Args <: HList, ArgParams <: HList](
    args: HNil.type, params: ArgParams
  )(implicit ev: FlatMapper.Aux[Args.Convert.type, ROUTE, HNil],
            ev2: FlatMapper.Aux[Params.Convert.type, Params, ArgParams]
  ): String = url(args: HNil, params)

  def unapply[Args <: HList, ArgParams <: HList](uri: String)(
    implicit ev: FlatMapper.Aux[Args.Convert.type, ROUTE, Args],
            ev2: FlatMapper.Aux[Params.Convert.type, Params, ArgParams]
  ): Option[(Args, ArgParams)] = parse(uri)

  def url[Args <: HList, ArgParams <: HList](args: Args, argParams: ArgParams)(
    implicit ev: FlatMapper.Aux[Args.Convert.type, ROUTE, Args],
             ev2: FlatMapper.Aux[Params.Convert.type, Params, ArgParams]
  ): String = {
    def compose(acc: String, arg: Codec[Any], name: String, value: Any): String = {
      val ampersand = if (acc.isEmpty) "?" else s"$acc&"
      val encoded   = arg.encode(value)
      ampersand + name + "=" + URI.encode(encoded)
    }

    def build[R <: HList, A <: HList](r: R, a: A)(sb: String): String =
      (r, a) match {
        case ((ph: ParamOpt[_]) :: pt, Some(vh) :: vt) =>
          build(pt, vt)(compose(sb, ph.asInstanceOf[ParamOpt[Any]].codec,
            ph.name, vh))
        case ((ph: ParamOpt[_]) :: pt, None :: vt) =>
          build(pt, vt)(sb)
        case ((ph: Param[_]) :: pt, vh :: vt) =>
          build(pt, vt)(compose(sb, ph.asInstanceOf[Param[Any]].codec,
            ph.name, vh))
        case ((ph: Fragment[_]) :: pt, vh :: vt) =>
          build(pt, vt)(
            sb + "#" + ph.asInstanceOf[Fragment[Any]].codec.encode(vh))
        case _ => sb
      }

    route(args) + build[Params, ArgParams](params, argParams)("")
  }

  def url[Args <: HList, ArgParams <: HList](
    args: HNil.type, params: ArgParams
  )(implicit ev: FlatMapper.Aux[Args.Convert.type, ROUTE, HNil],
            ev2: FlatMapper.Aux[Params.Convert.type, Params, ArgParams]
  ): String = url(args: HNil, params)

  def parse[Args <: HList, ArgParams <: HList]
    (uri: String)
    (implicit ev: FlatMapper.Aux[Args.Convert.type, ROUTE, Args],
             ev2: FlatMapper.Aux[Params.Convert.type, Params, ArgParams]
    ): Option[(Args, ArgParams)] =
  {
    val p = PathParser.parse(uri)
    for {
      parts <- route.parse(p.path)
      args  <- parseQuery(p.args, p.fragment)
    } yield (parts, args)
  }

  def &[T](param: Param[T])(
    implicit prepend: Prepend[Params, Param[T] :: HNil]
  ) = copy(params = params :+ param)

  def &[T](param: ParamOpt[T])(
    implicit prepend: Prepend[Params, ParamOpt[T] :: HNil]
  ) = copy(params = params :+ param)

  def &[T](param: Fragment[T])(
    implicit prepend: Prepend[Params, Fragment[T] :: HNil]
  ) = copy(params = params :+ param)

  private[trail] def parseQuery[Args <: HList](args: List[(String, String)],
                                               fragment: Option[String])(
    implicit ev: FlatMapper.Aux[Params.Convert.type, Params, Args]
  ): Option[Args] = {
    def m[R <: HList](r: R, s: List[(String, String)]): Option[HList] =
      r match {
        case HNil => Some(HNil)
        case (ph: Param[_]) :: pt =>
          for {
            result <- s.find(_._1 == ph.name)
            decode <- ph.codec.decode(result._2)
            tail   <- m(pt, s.diff(List(result)))
          } yield decode :: tail
        case (ph: ParamOpt[_]) :: pt =>
          s.find(_._1 == ph.name) match {
            case None => m(pt, s).map(None :: _)
            case Some(result) =>
              val acc   = m(pt, s.diff(List(result)))
              val value = ph.codec.decode(result._2)
              acc.map(value :: _)
          }
        case (ph: Fragment[_]) :: pt =>
          for {
            f      <- fragment
            decode <- ph.codec.decode(f)
          } yield decode :: HNil
      }

    m(params, args).map(_.asInstanceOf[Args])
  }
}
