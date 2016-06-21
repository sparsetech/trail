package pl.metastack.metarouter

import scala.annotation.implicitNotFound

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

