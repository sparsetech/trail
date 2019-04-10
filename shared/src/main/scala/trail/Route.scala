package trail

sealed trait Route[Args] {
  def url(args: Args): String
  def parseInternal(path: Path): Option[(Args, Path)]

  def parse(path: Path): Option[Args] = parseInternal(path).map(_._1)
  def parse(uri: String): Option[Args] =
    parseInternal(PathParser.parse(uri)).map(_._1)

  def apply(value: Args): String = url(value)
  def unapply(path: Path): Option[Args] = parse(path)
  def unapply(uri: String): Option[Args] = parse(uri)
}

case object Root extends Route[Unit] {
  override def url(value: Unit): String = "/"
  override def parseInternal(path: Path): Option[(Unit, Path)] =
    if (!path.path.startsWith("/")) None
    else Some((), path.copy(path = path.path.tail))
}

object Route {
  implicit class Route0Extensions(route: Route[Unit]) {
    def /[T](arg: Arg[T]): Route.ConcatRight[T] =
      Route.ConcatRight(route, Route.Dynamic(arg))
    def /[T](value: T)(implicit staticElement: StaticElement[T]): Route.ConcatRight[Unit] =
      Route.ConcatRight(route, Route.Static(staticElement.f(value)))
    def &[T](param: Param[T]): ParamRoute0[T] =
      ParamRoute0(route, param)
    def $[T](fragment: Fragment[T]): FragmentRoute0[T] =
      FragmentRoute0(route, fragment)
  }

  implicit class RouteNExtensions[P](route: Route[P]) {
    def /[T](arg: Arg[T]): Route.ConcatBoth[P, T] =
      Route.ConcatBoth(route, Route.Dynamic(arg))
    def /[T](value: T)(implicit staticElement: StaticElement[T]): Route.ConcatLeft[P] =
      Route.ConcatLeft(route, Route.Static(staticElement.f(value)))
    def &[T](param: Param[T]): ParamRoute[P, T] = ParamRoute(route, param)
    def $[T](fragment: Fragment[T]): FragmentRoute[P, T] =
      FragmentRoute(route, fragment)
  }

  case class Static(element: String) extends Route[Unit] {
    require(!element.contains("/"), "Element must not contain a slash")

    override def url(value: Unit): String = element
    override def parseInternal(path: Path): Option[(Unit, Path)] = {
      val untilSlash = path.path.takeWhile(_ != '/')
      if (untilSlash != element) None
      else Some(((), path.copy(path = path.path.drop(untilSlash.length + 1))))
    }
  }

  case class Dynamic[T](arg: Arg[T]) extends Route[T] {
    override def url(value: T): String = arg.codec.encode(value).getOrElse("")
    override def parseInternal(path: Path): Option[(T, Path)] = {
      val untilSlash = path.path.takeWhile(_ != '/')
      arg.codec.decode(Some(untilSlash))
        .map(value => (value, path.copy(path =
          path.path.drop(untilSlash.length + 1))))
    }
  }

  case class ConcatLeft[L](left: Route[L], right: Route[Unit]) extends Route[L] {
    override def url(value: L): String = left.url(value) + "/" + right.url(())
    override def parseInternal(path: Path): Option[(L, Path)] =
      for {
        (lv, lp) <- left.parseInternal(path)
        (_ , rp) <- right.parseInternal(lp)
      } yield (lv, rp)
  }

  case class ConcatRight[R](left: Route[Unit], right: Route[R]) extends Route[R] {
    override def url(value: R): String =
      (if (left == Root) "" else left.url(())) + "/" + right.url(value)
    override def parseInternal(path: Path): Option[(R, Path)] =
      for {
        (_ , lp) <- left.parseInternal(path)
        (rv, rp) <- right.parseInternal(lp)
      } yield (rv, rp)
  }

  case class ConcatBoth[L, R](left: Route[L], right: Route[R]) extends Route[(L, R)] {
    override def url(value: (L, R)): String = left.url(value._1) + "/" + right.url(value._2)
    override def parseInternal(path: Path): Option[((L, R), Path)] =
      for {
        (lv, lp) <- left.parseInternal(path)
        (rv, rp) <- right.parseInternal(lp)
      } yield ((lv, rv), rp)
  }

  case class FragmentRoute0[P](route: Route[Unit], fragment: Fragment[P]) extends Route[P] {
    override def url(value: P): String =
      fragment.codec.encode(value) match {
        case None => route.url(())
        case Some(frag) => route.url(()) + "#" + frag
      }
    override def parseInternal(path: Path): Option[(P, Path)] =
      for {
        (_, lp) <- route.parseInternal(path)
        v <- fragment.codec.decode(lp.fragment)
        p = lp.copy(fragment = None)
      } yield (v, p)
  }

  case class FragmentRoute[A, P](route: Route[A], fragment: Fragment[P]) extends Route[(A, P)] {
    override def url(value: (A, P)): String =
      fragment.codec.encode(value._2) match {
        case None => route.url(value._1)
        case Some(frag) => route.url(value._1) + "#" + frag
      }
    override def parseInternal(path: Path): Option[((A, P), Path)] =
      for {
        (lv, lp) <- route.parseInternal(path)
        rv <- fragment.codec.decode(lp.fragment)
        p = lp.copy(fragment = None)
      } yield ((lv, rv), p)
  }

  case class ParamRoute0[P](route: Route[Unit], param: Param[P]) extends Route[P] {
    override def url(value: P): String = {
      val arg = param.codec.encode(value)
        .fold("")(v => param.name + "=" + URI.encode(v))
      route.url(()) + (if (arg.isEmpty) "" else "?" + arg)
    }
    override def parseInternal(path: Path): Option[(P, Path)] =
      for {
        (_, lp) <- route.parseInternal(path)
        arg = lp.args.find(_._1 == param.name)
        v <- param.codec.decode(arg.map(_._2))
        p = lp.copy(args = lp.args.diff(arg.toList))
      } yield (v, p)
    def &[T](param: Param[T]): ParamRoute[P, T] = ParamRoute(this, param)
  }

  case class ParamRoute[A, P](route: Route[A], param: Param[P]) extends Route[(A, P)] {
    override def url(value: (A, P)): String = {
      val base = route.url(value._1)
      val encodedParam =
        param.codec.encode(value._2).map(v => param.name + "=" + URI.encode(v))
          .getOrElse("")
      val delimiter = if (base.contains("?")) "&" else "?"
      base + delimiter + encodedParam
    }
    override def parseInternal(path: Path): Option[((A, P), Path)] =
      for {
        (lv, lp) <- route.parseInternal(path)
        arg = lp.args.find(_._1 == param.name)
        rv <- param.codec.decode(arg.map(_._2))
        p = lp.copy(args = lp.args.diff(arg.toList))
      } yield ((lv, rv), p)
  }
}
