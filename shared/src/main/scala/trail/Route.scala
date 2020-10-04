package trail

sealed trait Route[Args] {
  def url(args: Args): String

  private def process(result: Option[(Args, Path)]): Option[Args] =
    result.collect { case (args, p) if p.path.isEmpty => args }

  private def processStrict(result: Option[(Args, Path)]): Option[Args] =
    result.collect { case (args, p) if
      p.path.isEmpty && p.args.isEmpty && p.fragment.isEmpty => args
    }

  /**
   * Parse given [[Path]]
   *
   * If successful, the result contains the parsed arguments as well as the
   * updated [[Path]] object. If the path was fully parsed, all of its fields
   * should be empty.
   *
   * Use [[parseArgs]] or [[parseArgsStrict]] if you are only interested in the
   * parsed arguments.
   *
   * @return Some((parsed arguments, updated path))
   */
  def parse(path: Path): Option[(Args, Path)]

  /**
   * Parse given URI
   *
   * @see parse(path: Path)
   */
  def parse(uri: String): Option[(Args, Path)] = parse(PathParser.parse(uri))

  /**
   * Parse arguments of given [[Path]]
   *
   * This returns None if `path` contains additional trailing elements missing
   * from the route.
   */
  def parseArgs(path: Path): Option[Args] = process(parse(path))

  /**
   * Parse arguments of given URI
   *
   * @see parseArgs(path: Path)
   */
  def parseArgs(uri: String): Option[Args] = process(parse(uri))

  /**
   * Parse arguments of given [[Path]]
   *
   * Same behaviour as [[parseArgs]]. It does not permit any unspecified path
   * elements, arguments or a fragment.
   */
  def parseArgsStrict(path: Path): Option[Args] = processStrict(parse(path))

  /**
   * Parse arguments of given URI
   *
   * @see parseArgsStrict(path: Path)
   */
  def parseArgsStrict(uri: String): Option[Args] = processStrict(parse(uri))

  /**
   * Generate URL for given arguments
   */
  def apply(args: Args): String = url(args)

  /**
   * Parse arguments for given [[Path]]
   *
   * @see [[parseArgs]]
   */
  def unapply(path: Path): Option[Args] = parseArgs(path)

  /**
   * Parse arguments for given URI
   *
   * @see [[parseArgs]]
   */
  def unapply(uri: String): Option[Args] = parseArgs(uri)

  def &(params: Params.type): Route.ParamsRoute[Args] = Route.ParamsRoute(this)
}

case object Root extends Route[Unit] {
  override def url(value: Unit): String = "/"
  override def parse(path: Path): Option[(Unit, Path)] =
    if (!path.path.startsWith("/")) None
    else Some(((), path.copy(path = path.path.tail)))
}

case object Elems extends Route[List[String]] {
  override def url(value: List[String]): String = value.mkString("/")
  override def parse(path: Path): Option[(List[String], Path)] =
    Some((PathParser.parseParts(path.path), path.copy(path = "")))
}

case object Params

object Route {
  implicit class Route0Extensions(route: Route[Unit]) {
    def /[T](arg: Arg[T]): Route.ConcatRight[T] =
      Route.ConcatRight(route, Route.Dynamic(arg))
    def /[T](value: T)(implicit staticElement: StaticElement[T]): Route.ConcatRight[Unit] =
      Route.ConcatRight(route, Route.Static(staticElement.f(value)))
    def /[T](rest: Elems.type): Route.ConcatRight[List[String]] =
      Route.ConcatRight(route, Elems)
    def &[T](param: Param[T]): ParamRoute0[T] =
      ParamRoute0(route, param)
    def &(params: Params.type): ParamsRoute[Unit] = ParamsRoute(route)
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
    override def parse(path: Path): Option[(Unit, Path)] = {
      val untilSlash = path.path.takeWhile(_ != '/')
      if (untilSlash != element) None
      else Some(((), path.copy(path = path.path.drop(untilSlash.length + 1))))
    }
  }

  case class Dynamic[T](arg: Arg[T]) extends Route[T] {
    override def url(value: T): String = arg.codec.encode(value).getOrElse("")
    override def parse(path: Path): Option[(T, Path)] = {
      val untilSlash = path.path.takeWhile(_ != '/')
      arg.codec.decode(if (untilSlash.isEmpty) None else Some(untilSlash))
        .map(value => (value, path.copy(path =
          path.path.drop(untilSlash.length + 1))))
    }
  }

  case class ConcatLeft[L](left: Route[L], right: Route[Unit]) extends Route[L] {
    override def url(value: L): String = left.url(value) + "/" + right.url(())
    override def parse(path: Path): Option[(L, Path)] =
      for {
        (lv, lp) <- left.parse(path)
        (_ , rp) <- right.parse(lp)
      } yield (lv, rp)
  }

  case class ConcatRight[R](left: Route[Unit], right: Route[R]) extends Route[R] {
    override def url(value: R): String =
      (if (left == Root) "" else left.url(())) + "/" + right.url(value)
    override def parse(path: Path): Option[(R, Path)] =
      for {
        (_ , lp) <- left.parse(path)
        (rv, rp) <- right.parse(lp)
      } yield (rv, rp)
  }

  case class ConcatBoth[L, R](left: Route[L], right: Route[R]) extends Route[(L, R)] {
    override def url(value: (L, R)): String = left.url(value._1) + "/" + right.url(value._2)
    override def parse(path: Path): Option[((L, R), Path)] =
      for {
        (lv, lp) <- left.parse(path)
        (rv, rp) <- right.parse(lp)
      } yield ((lv, rv), rp)
  }

  case class FragmentRoute0[P](route: Route[Unit], fragment: Fragment[P]) extends Route[P] {
    override def url(value: P): String =
      fragment.codec.encode(value) match {
        case None => route.url(())
        case Some(frag) => route.url(()) + "#" + frag
      }
    override def parse(path: Path): Option[(P, Path)] =
      for {
        (_, lp) <- route.parse(path)
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
    override def parse(path: Path): Option[((A, P), Path)] =
      for {
        (lv, lp) <- route.parse(path)
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
    override def parse(path: Path): Option[(P, Path)] =
      for {
        (_, lp) <- route.parse(path)
        arg = lp.args.find(_._1 == param.name)
        v <- param.codec.decode(arg.map(_._2))
        p = lp.copy(args = lp.args.diff(arg.toList))
      } yield (v, p)
    def &[T](param: Param[T]): ParamRoute[P, T] = ParamRoute(this, param)
  }

  case class ParamRoute[A, P](route: Route[A], param: Param[P]) extends Route[(A, P)] {
    override def url(value: (A, P)): String = {
      val base = route.url(value._1)
      param.codec.encode(value._2)
        .map(v => param.name + "=" + URI.encode(v))
        .fold(base) { encodedParam =>
          val delimiter = if (base.contains("?")) "&" else "?"
          base + delimiter + encodedParam
        }
    }
    override def parse(path: Path): Option[((A, P), Path)] =
      for {
        (lv, lp) <- route.parse(path)
        arg = lp.args.find(_._1 == param.name)
        rv <- param.codec.decode(arg.map(_._2))
        p = lp.copy(args = lp.args.diff(arg.toList))
      } yield ((lv, rv), p)
  }

  case class ParamsRoute[A](route: Route[A]) extends Route[(A, List[(String, String)])] {
    override def url(value: (A, List[(String, String)])): String = {
      val (valueRoute, args) = value
      val base = route.url(valueRoute)
      if (args.isEmpty) base
      else {
        val argsEncoded = args.map { case (n, v) => n + "=" + URI.encode(v) }
        base + (if (base.contains("?")) "&" else "?") + argsEncoded.mkString("&")
      }
    }

    override def parse(path: Path): Option[((A, List[(String, String)]), Path)] =
      for {
        (lv, lp) <- route.parse(path)
        p = lp.copy(args = List())
      } yield ((lv, lp.args), p)
  }
}
