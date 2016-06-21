package pl.metastack.metarouter.manual

import pl.metastack.metadocs.SectionSupport

object Examples extends SectionSupport {
  section("parse") {
    import pl.metastack.metarouter._

    case class Details(userId: Int)
    case class UserInfo(user: String, details: Boolean)

    val details  = (Root / "details" / Arg[Int]).as[Details]
    val userInfo = (Root / "user" / Arg[String] / Arg[Boolean]).as[UserInfo]

    val routes = ComposedRoute(details).orElse(userInfo)

    routes.parse("/user/hello/false")
  }
}
