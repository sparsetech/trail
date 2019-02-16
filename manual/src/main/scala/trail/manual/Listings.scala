package trail.manual

import leaf.notebook._

object Listings extends App {
  implicit val session = Session()

  listing("route")
  import trail._
  import shapeless._

  val details = Root / "details" / Arg[Int]
  println(details)

  listing("url")
  println(details.url(1 :: HNil))  // Shorter: details(1 :: HNil)

  listing("map")
  println(details.parse("/details/42"))

  listing("query-params")
  val route = Root / "details" & Param[Boolean]("show")
  println(route.parse("/details?show=false"))

  listing("query-params-opt")
  val routeParamsOpt = Root / "details" & Param[Int]("id") & ParamOpt[Boolean]("show")
  println(routeParamsOpt.parse("/details?id=42"))

  listing("query-fragment")
  val routeFragment = Root & Fragment[Int]
  println(routeFragment.parse("/#42"))

  listing("parse")
  val userInfo = Root / "user" / Arg[String] & Param[Boolean]("show")

  val result = "/user/hello?show=false" match {
    case details (a :: HNil)            => s"details: $a"
    case userInfo(u :: HNil, s :: HNil) => s"user: $u, show: $s"
  }
  println(result)

  listing("parse-path")
  val result2 = trail.Path("/user/hello", List("show" -> "false")) match {
    case details (a :: HNil)            => s"details: $a"
    case userInfo(u :: HNil, s :: HNil) => s"user: $u, show: $s"
  }
  println(result2)

  listing("custom-arg")
  import scala.util.Try
  implicit case object IntSetArg extends Codec[Set[Int]] {
    override def encode(s: Set[Int]): String = s.mkString(",")
    override def decode(s: String): Option[Set[Int]] =
      Try(s.split(',').map(_.toInt).toSet).toOption
  }

  val export = Root / "export" / Arg[Set[Int]]
  println(export.url(Set(1, 2, 3) :: HNil))

  listing("custom-path-elem")
  case class Foo(bar: String)
  implicit object FooElement extends StaticElement[Foo](_.bar)

  println((Root / Foo("asdf")).url())

  end()
  write("manual/listings.json")
}
