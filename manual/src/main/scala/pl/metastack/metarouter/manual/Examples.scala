package pl.metastack.metarouter.manual

import pl.metastack.metadocs.SectionSupport

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

    val routes = Router.create(details).orElse(userInfo)

    routes.parse("/user/hello/false").map {
      case (`details`,  (a: Int) :: HNil)                    => s"details: $a"
      case (`userInfo`, (u: String) :: (d: Boolean) :: HNil) => s"user: $u / $d"
      case _                                                 => ""
    }
  }
}
