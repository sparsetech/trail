package trail.manual

import java.io.{File, FileWriter}

import pl.metastack.metadocs._

object Listings extends App {
  import Notebook._
  implicit val session = Session()

  block("route")
  import trail._
  import shapeless._

  val details = Root / "details" / Arg[Int]
  println(details)

  block("url")
  println(details.url(1 :: HNil))  // Shorter: details(1 :: HNil)

  block("map")
  println(details.parse("/details/42"))

  block("query-params")
  val route = Root / "details" & Param[Boolean]("show")
  println(route.parse("/details?show=false"))

  block("query-params-opt")
  val routeParamsOpt = Root / "details" & Param[Int]("id") & ParamOpt[Boolean]("show")
  println(routeParamsOpt.parse("/details?id=42"))

  block("parse")
  val userInfo = Root / "user" / Arg[String] & Param[Boolean]("show")

  val result = "/user/hello?show=false" match {
    case details (a :: HNil)            => s"details: $a"
    case userInfo(u :: HNil, s :: HNil) => s"user: $u, show: $s"
  }
  println(result)

  block("custom-arg")
  import scala.util.Try
  implicit case object IntSetArg extends Codec[Set[Int]] {
    override def encode(s: Set[Int]): String = s.mkString(",")
    override def decode(s: String): Option[Set[Int]] =
      Try(s.split(',').map(_.toInt).toSet).toOption
  }

  val export = Root / "export" / Arg[Set[Int]]
  println(export.url(Set(1, 2, 3) :: HNil))

  block("custom-path-elem")
  case class Foo(bar: String)
  implicit object FooElement extends StaticElement[Foo](_.bar)

  println((Root / Foo("asdf")).url())

  end()

  val blocks = session.serialiseBlocks()
  println(s"Found ${blocks.size} blocks")

  val output = new File("manual/listings.json")

  import io.circe._, io.circe.generic.auto._, io.circe.parser._, io.circe.syntax._
  val fw = new FileWriter(output)
  fw.write(blocks.asJson.spaces2)
  fw.close()
}
