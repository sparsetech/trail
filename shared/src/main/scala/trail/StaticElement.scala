package trail

import scala.annotation.implicitNotFound

@implicitNotFound("${T} cannot be used as a path element. Define an instance StaticElement[${T}].")
class StaticElement[T](val f: T => String)

object StaticElement {
  implicit object StringElement  extends StaticElement[String ](identity)
  implicit object BooleanElement extends StaticElement[Boolean](_.toString)
  implicit object IntElement     extends StaticElement[Int    ](_.toString)
  implicit object LongElement    extends StaticElement[Long   ](_.toString)
}
