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

  section("parse") {
    val details  = Root / "details" / Arg[Int]
    val userInfo = Root / "user" / Arg[String] / Arg[Boolean]

    "/user/hello/false" match {
      case `details` (a :: HNil)      => s"details: $a"
      case `userInfo`(u :: d :: HNil) => s"user: $u/$d"
    }
  }

  section("custom-arg") {
    implicit case object IntSetArg extends ParseableArg[Set[Int]] {
      override def urlDecode(s: String) =
        Try(s.split(",").map(_.toInt).toSet).toOption
      override def urlEncode(s: Set[Int]) = s.mkString(",")
    }

    val export = Root / "export" / Arg[Set[Int]]
    Router.url(export, Set(1, 2, 3) :: HNil)
  }
}
