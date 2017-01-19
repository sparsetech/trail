package pl.metastack.metarouter.manual

import pl.metastack.metadocs.SectionSupport

object Examples extends SectionSupport {
  section("parse") {
    import pl.metastack.metarouter._
    import Router.route

    case class Details(userId: Int)
    case class UserInfo(user: String, details: Boolean)

    val details  = route[Details](Root / "details" / Arg[Int])
    val userInfo = route[UserInfo](Root / "user" / Arg[String] / Arg[Boolean])

    val routes = ComposedRoute(details).orElse(userInfo)

    routes.parse("/user/hello/false")
  }
}
