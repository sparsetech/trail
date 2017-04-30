package pl.metastack.metarouter.manual

import pl.metastack.metadocs.SectionSupport

import scala.util.Try

object Examples extends SectionSupport {
  section("route") {
    import shapeless._
    import pl.metastack.metarouter._

    val details = Root / "details" / Arg[Int]
    details
  }

  import shapeless._
  import pl.metastack.metarouter._
  val details = Root / "details" / Arg[Int]

  section("url") {
    Router.url(details, 1 :: HNil)
  }

  section("map") {
    Router.parse(details, "/details/42")
  }

  section("query-params") {
    val route = Root / "details" & Param[Boolean]("show")
    Router.parse(route, "/details?show=false")
  }

  section("query-params-opt") {
    val route = Root / "details" & Param[Int]("id") & ParamOpt[Boolean]("show")
    Router.parse(route, "/details?id=42")
  }

  section("parse") {
    val details  = Root / "details" / Arg[Int]
    val userInfo = Root / "user" / Arg[String] & Param[Boolean]("show")

    "/user/hello?show=false" match {
      case `details` (a :: HNil)            => s"details: $a"
      case `userInfo`(u :: HNil, s :: HNil) => s"user: $u, show: $s"
    }
  }

  section("custom-arg") {
    implicit case object IntSetArg extends Codec[Set[Int]] {
      override def decode(s: String) =
        Try(s.split(",").map(_.toInt).toSet).toOption
      override def encode(s: Set[Int]) = s.mkString(",")
    }

    val export = Root / "export" / Arg[Set[Int]]
    Router.url(export, Set(1, 2, 3) :: HNil)
  }
}
