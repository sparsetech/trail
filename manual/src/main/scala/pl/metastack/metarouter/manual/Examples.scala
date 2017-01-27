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
    case class Details(userId: Int)
    val details = Router.route[Details](Root / "details" / Arg[Int])
    Router.parse(details, "/details/42")
  }

  section("parse") {
    case class Details(userId: Int)
    case class UserInfo(user: String, details: Boolean)

    val details  = Router.route[Details](Root / "details" / Arg[Int])
    val userInfo = Router.route[UserInfo](Root / "user" / Arg[String] / Arg[Boolean])

    val routes = Router.create(details).orElse(userInfo)
    routes.parse("/user/hello/false")
  }

  case class Details(userId: Int)
  case class UserInfo(user: String, details: Boolean)

  section("urls") {
    val details  = Router.route[Details](Root / "details" / Arg[Int])
    val userInfo = Router.route[UserInfo](Root / "user" / Arg[String] / Arg[Boolean])

    List(
      Router.url(details, Details(42)),
      Router.url(userInfo, UserInfo("test", true))
    )
  }

  section("urls-implicit") {
    implicit val details  = Router.route[Details](Root / "details" / Arg[Int])
    implicit val userInfo = Router.route[UserInfo](Root / "user" / Arg[String] / Arg[Boolean])

    List(
      Router.url(Details(42)),
      Router.url(UserInfo("test", true))
    )
  }
}
