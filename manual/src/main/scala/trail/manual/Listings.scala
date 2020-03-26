package trail.manual

import leaf.notebook._

object Listings extends App {
  implicit val session = Session()

  listing("route")
  import trail._
  val details = Root / "details" / Arg[Int]

  listing("url")
  println(details.url(1))
  println(details(1))

  listing("parse")
  println(details.parse("/details/42"))
  println(details.parse("/details/42/sub-page?name=value"))
  println(details.parse("/details/42/sub-page?name=value#frag"))

  listing("query-params")
  val route = Root / "details" & Param[Boolean]("show")
  println(route.parseArgs("/details/sub-page"))
  println(route.parseArgs("/details?show=false"))
  println(route.parseArgs("/details?show=false&a=b"))
  println(route.parseArgs("/details#frag"))

  listing("query-params-strict")
  println(route.parseArgsStrict("/details/sub-page"))
  println(route.parseArgsStrict("/details?show=false"))
  println(route.parseArgsStrict("/details?show=false&a=b"))
  println(route.parseArgsStrict("/details#frag"))

  listing("query-params-opt")
  val routeParamsOpt = Root / "details" & Param[Int]("id") & Param[Option[Boolean]]("show")
  println(routeParamsOpt.parseArgs("/details?id=42"))

  listing("query-fragment")
  val routeFragment = Root $ Fragment[Int]
  println(routeFragment.parseArgs("/#42"))

  listing("additional-elems")
  val routeAdditionalElems = Root / "a" / Elems
  println(routeAdditionalElems.parseArgs("/a/b/c"))

  listing("additional-params")
  val routeAdditionalParams = Root / Arg[String] & Params
  println(routeAdditionalParams.parseArgs("/a?param1=value1&param2=value2"))

  listing("routing-table")
  val userInfo = Root / "user" / Arg[String] & Param[Boolean]("show")

  val result = "/user/hello?show=false" match {
    case details (a)      => s"details: $a"
    case userInfo((u, s)) => s"user: $u, show: $s"
  }
  println(result)

  listing("parse-path")
  val (requestPath, requestParams) = ("/user/hello", List("show" -> "false"))
  val result2 = trail.Path(requestPath, requestParams) match {
    case details (a)      => s"details: $a"
    case userInfo((u, s)) => s"user: $u, show: $s"
  }
  println(result2)

  listing("custom-codec")
  import scala.util.Try
  implicit case object IntSetCodec extends Codec[Set[Int]] {
    override def encode(s: Set[Int]): Option[String] = Some(s.mkString(","))
    override def decode(s: Option[String]): Option[Set[Int]] =
      s.flatMap(value =>
        if (value.isEmpty) Some(Set())
        else Try(value.split(',').map(_.toInt).toSet).toOption)
  }

  val export = Root / "export" / Arg[Set[Int]]
  println(export.url(Set(1, 2, 3)))

  listing("custom-path-elem")
  case class Foo(bar: String)
  implicit object FooElement extends StaticElement[Foo](_.bar)

  println((Root / Foo("asdf")).url(()))

  listing("uri-values")
  val encoded = URI.encode("äöü")
  println(encoded)
  println(URI.decode(encoded))

  end()
  write("manual/listings.json")
}
