package pl.metastack.metarouter

import scala.annotation.implicitNotFound
import scala.collection.immutable
import scala.util.Try
import scala.util.matching.Regex

import shapeless._
import shapeless.ops.hlist._

object Route {
  // TODO: Figure out what to do with relative routes.
  def parse(s: String) = {
    val r = Route(split(s).foldRight[HList](HNil)((x,y)=> x :: y))
    InstantiatedRoute(r,HNil)
  }
  def split(s: String): immutable.List[String] = s.stripPrefix("/").split('/').toList

  trait Drop extends Poly1 {
    implicit def default[T] = at[T](x => HNil)
  }
  object ConvertArgs extends Drop {
    implicit def caseSome[T] = at[Arg[T]].apply[T :: HNil](x => (null.asInstanceOf[T]) :: HNil)
  }

  implicit class CanFillRoute[D <: HList, A <: HList](val d: Route[D])(implicit val f: FlatMapper.Aux[ConvertArgs.type, D, A]) extends ProductArgs {
    def fill = this
    def applyProduct(it: A): InstantiatedRoute[D, A] = InstantiatedRoute[D, A](d,it)

    def matches(s: String): Either[String, InstantiatedRoute[D,A]] = {
      val parts = split(s)
      def m[R <: HList](r: R, s: Seq[String]): Either[String, HList] = {
        import shapeless.HList.ListCompat._
        import scala.collection.immutable.::
        (r,s) match {
          case (HNil, Nil) => Right(HNil)
          case (_, Nil) => Left(s"""Path is to short.""")
          case (HNil, _) => Left(s"""Path is to long.""")
          case ((rH: String) #: rT, sH :: sT) if rH == sH => m(rT, sT)
          case ((rH: String) #: rT, sH :: sT) => Left(s"""Path Element "$rH" did not match "$sH"""")
          case ((arg: Arg[_]) #: rT, sH :: sT) =>
            val decoded = arg.parseableArg.urlDecode(sH)
            if(decoded.isDefined) {
              m(rT, sT).right.map(decoded.get :: _)
            } else {
              Left[String, HNil](s"""Argument "${arg.name}" could not parse "$sH".""")
            }
        }
      }
      m[D](d.pathElements, parts).right.map { x =>
        // Using asInstanceOf as a band aid since compiler isn't able to confirm the type.
        // Based on the logic in the above match it will be correct or its not a Right Either.
        InstantiatedRoute(d, x.asInstanceOf[A])
      }
    }
  }

  val Root = Route[HNil](HNil)
}

case class Route[ROUTE <: HList] private (val pathElements: ROUTE) {
  def /[T, E](a: T)(implicit pe: PathElement.Aux[T,E], prepend : Prepend[ROUTE, E :: HNil]) =
    Route(this.pathElements :+ pe.toPathElement(a))

  override def canEqual(that: Any): Boolean = that.isInstanceOf[Route[ROUTE]]

  override def equals(obj: scala.Any): Boolean = {
    def cmp(ths: HList, that: HList): Boolean = {
      (ths, that) match {
        case (HNil, HNil) => true
        case (_   , HNil) => false
        case (HNil, _   ) => false
        case ((aH: Arg[_]) :: aT, (bH: Arg[_]) :: bT) if aH.parseableArg == bH.parseableArg => cmp(aT,bT)
        case ((aH: Arg[_]) :: aT, (bH: Arg[_]) :: bT) if aH.parseableArg != bH.parseableArg => false
        case (aH :: aT, bH :: bT) if aH == bH => cmp(aT,bT)
        case (aH :: aT, bH :: bT) => false
      }
    }
    if(canEqual(obj))
      cmp(this.pathElements, obj.asInstanceOf[Route[HList]].pathElements)
    else false
  }

  override def hashCode(): Int = {
    def build[R <: HList](p: R): Int = {
      p match {
        case HNil => 0
        case (h: Arg[_]) :: t => h.parseableArg.hashCode() ^ build(t)
        case h :: t => h.hashCode() ^ build(t)
      }
    }
    build(pathElements)
  }
}

case class InstantiatedRoute[ROUTE <: HList, DATA <: HList] private[metarouter] (val route: Route[ROUTE], val data: DATA)  {
  def url(): String = {
    def build[R <: HList, A <: HList](r: R, a: A)(sb: String): String = {
      (r,a) match {
        case (HNil, HNil) if sb.isEmpty => "/"
        case (HNil, HNil) => sb
        case ((h: String) :: t, _) => build(t, a)(s"$sb/$h")
        case ((a: Arg[_]) :: t, ah :: at) => build(t, at)(s"$sb/${a.asInstanceOf[Arg[Any]].parseableArg.urlEncode(ah)}")
      }
    }
    build[ROUTE, DATA](route.pathElements, data)("")
  }

  override def canEqual(that: Any): Boolean = that.isInstanceOf[InstantiatedRoute[ROUTE,DATA]]

  override def equals(that: Any): Boolean =
    if(canEqual(that)) this.url == that.asInstanceOf[InstantiatedRoute[_,_]].url
    else false

  override def hashCode(): Int = url.hashCode
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
    type E = T
    def toPathElement(t: T): E = t
  }

  implicit object StringStaticElement extends StaticElement[String] {
    def urlEncode(value: String): String = value
  }
  implicit object BooleanStaticElement extends StaticElement[Boolean] {
    def urlEncode(value: Boolean): String = value.toString
  }
  implicit object IntStaticElement extends StaticElement[Int] {
    def urlEncode(value: Int): String = value.toString
  }
}

// TODO: Write test for implicitNotFound Message
@implicitNotFound("${T} cannot be used as a Element in a Route path. (Missing typeclass instance for StaticElement[${T}])")
trait StaticElement[T] extends PathElement[T] {
  type E = String
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
    def urlDecode(s: String) = Try(s.toBoolean).toOption
    def urlEncode(s: Boolean): String = s.toString
  }
  implicit case object IntArg extends ParseableArg[Int] {
    def urlDecode(s: String) = Try(s.toInt).toOption
    def urlEncode(s: Int): String = s.toString
  }
  implicit case object StringArg extends ParseableArg[String] {
    def urlDecode(s: String) = Option(s)
    def urlEncode(s: String): String = s
  }
}
