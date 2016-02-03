package pl.metastack.metarouter

import scala.annotation.implicitNotFound
import scala.util.Try

import shapeless._
import shapeless.ops.hlist._

object Route {
  // TODO: Figure out what to do with relative routes.
  def parse(s: String) = {
    val r = Route(split(s).foldRight[HList](HNil)((x, y) => x :: y))
    InstantiatedRoute(r, HNil)
  }

  def split(s: String): List[String] = s.stripPrefix("/").split('/').toList

  trait Drop extends Poly1 {
    implicit def default[T] = at[T](x => HNil)
  }
  object ConvertArgs extends Drop {
    implicit def caseArg[T] = at[Arg[T]].apply[T :: HNil](x => null.asInstanceOf[T] :: HNil)
  }

  val Root = Route[HNil](HNil)
}

case class Route[ROUTE <: HList] private (val pathElements: ROUTE) {
  def fill()(implicit map: FlatMapper.Aux[Route.ConvertArgs.type, ROUTE, HNil]):
    InstantiatedRoute[ROUTE, HNil] =
      InstantiatedRoute[ROUTE, HNil](this, HNil)

  def fill[T](arg: T)
    (implicit map: FlatMapper.Aux[Route.ConvertArgs.type, ROUTE, T :: HNil]):
    InstantiatedRoute[ROUTE, T :: HNil] =
      InstantiatedRoute[ROUTE, T :: HNil](this, arg :: HNil)

  def fillN[L <: HList, TP <: Product](args: TP)
    (implicit hl: Generic.Aux[TP, L],
              map: FlatMapper.Aux[Route.ConvertArgs.type, ROUTE, L]
    ): InstantiatedRoute[ROUTE, L] =
      InstantiatedRoute[ROUTE, L](this, hl.to(args))

  def matches(s: String): Either[String, InstantiatedRoute[ROUTE, HList]] = {
    import shapeless.HList.ListCompat._

    def m[R <: HList](r: R, s: Seq[String]): Either[String, HList] =
      (r, s) match {
        case (HNil, Nil) => Right(HNil)
        case (_, Nil) => Left(s"Path is too short.")
        case (HNil, _) => Left(s"Path is too long.")
        case ((rH: String) #: rT, sH :: sT) if rH == sH => m(rT, sT)
        case ((rH: String) #: rT, sH :: sT) => Left(s"Path element `$rH` did not match `$sH`")
        case ((arg: Arg[_]) #: rT, sH :: sT) =>
          arg.parseableArg.urlDecode(sH) match {
            case Some(decoded) => m(rT, sT).right.map(decoded :: _)
            case _ => Left[String, HNil](s"Argument `${arg.name}` could not parse `$sH`.")
          }
      }

    m[ROUTE](pathElements, Route.split(s)).right.map { x =>
      InstantiatedRoute(this, x)
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

case class InstantiatedRoute[ROUTE <: HList, DATA <: HList] private[metarouter] (val route: Route[ROUTE], val data: DATA) {
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

trait PathElement[T] {
  type E
  def toPathElement(t: T): E
}
object PathElement {
  type Aux[T, Elm] = PathElement[T] {
    type E = Elm
  }

  implicit def ArgsElement[T <: Arg[_]] = new PathElement[T] {
    override type E = T
    override def toPathElement(t: T) = t
  }

  implicit object StringStaticElement extends StaticElement[String] {
    override def urlEncode(value: String) = value
  }
  implicit object BooleanStaticElement extends StaticElement[Boolean] {
    override def urlEncode(value: Boolean) = value.toString
  }
  implicit object IntStaticElement extends StaticElement[Int] {
    override def urlEncode(value: Int) = value.toString
  }
}

// TODO: Write test for implicitNotFound Message
@implicitNotFound("${T} cannot be used as a Element in a Route path. (Missing typeclass instance for StaticElement[${T}])")
trait StaticElement[T] extends PathElement[T] {
  override type E = String
  def urlEncode(value: T): String
  def toPathElement(t: T): String = urlEncode(t)
}

case class Arg[T](name: String)(implicit val parseableArg: ParseableArg[T])

trait ParseableArg[T] {
  def urlDecode(s: String): Option[T]
  def urlEncode(s: T): String
}
object ParseableArg {
  implicit case object BooleanArg extends ParseableArg[Boolean] {
    override def urlDecode(s: String) = Try(s.toBoolean).toOption
    override def urlEncode(s: Boolean) = s.toString
  }
  implicit case object IntArg extends ParseableArg[Int] {
    override def urlDecode(s: String) = Try(s.toInt).toOption
    override def urlEncode(s: Int) = s.toString
  }
  implicit case object StringArg extends ParseableArg[String] {
    override def urlDecode(s: String) = Option(s)
    override def urlEncode(s: String) = s
  }
}
