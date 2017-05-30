package trail

import scala.annotation.implicitNotFound

@implicitNotFound("${T} cannot be used as a path element. Define an instance StaticElement[${T}].")
class PathElement[T, U](val f: T => U)

class StaticElement[T](f: T => String) extends PathElement[T, String](f)

object PathElement {
  implicit def argElement[T] = new PathElement[Arg[T], Arg[T]](identity)

  implicit object StringElement  extends StaticElement[String ](identity)
  implicit object BooleanElement extends StaticElement[Boolean](_.toString)
  implicit object IntElement     extends StaticElement[Int    ](_.toString)
  implicit object LongElement    extends StaticElement[Long   ](_.toString)
}
