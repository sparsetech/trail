package pl.metastack.metarouter

import org.scalatest._

import pl.metastack.metarouter._
import shapeless.HNil

class RouteExampleTests extends FlatSpec with Matchers {
  "A Simple example" should "just work" ignore {
    ???
    /*val Details : Route[Int] = "details" / Arg[Int]("contentId")
    val UserInfo: Route[(String, Boolean)] = "user" / Arg[String]("user") / Arg[Boolean]("details")

    val userInfo: InstantiatedRoute[(String, Boolean)] = UserInfo("bob", false)
    userInfo == "user" / "bob" / false
    userInfo.url == "user/bob/false"

    Route.parse("a/b/c") == "a" / "b" / "c"

    userInfo.matches(UserInfo) == true

    ("user" / "bob").errorMessage(UserInfo) == Some("`details` not specified")

    val router = new Router(Seq(Details, UserInfo))
    val matchingRoutes = Seq[Route[_]] = router.filter(userInfo)
    matchingRoutes == Seq(UserInfo)*/
  }

  "Matching root" should "just work" in {
    Root.parse("/") shouldBe defined
  }

  "A Modified Simple example" should "just work" in {
    val UserInfo = Root / "user" / Arg[String] / Arg[Boolean]
    val userInfo = Router.fill(UserInfo, "bob" :: false :: HNil)

    assert(Router.url(userInfo) === "/user/bob/false")
    assert(userInfo.route === UserInfo)

    assert(Router.url(userInfo) === Router.url(Router.fill(Root / "user" / "bob" / false)))

    assert(Route.parse("/a/b/c") === Router.fill(Root / "a" / "b" / "c", HNil))

    UserInfo.parse("/user/bob/true") shouldBe defined
    val parsedRoute = UserInfo.parse("/user/bob/true").get
    assert(parsedRoute.route === UserInfo)
    assert(parsedRoute.data == "bob" :: true :: HNil)

    UserInfo.parse("/user/bob") shouldBe empty
    UserInfo.parse("/user/bob/true/true") shouldBe empty
    UserInfo.parse("/user/bob/1") shouldBe empty
    UserInfo.parse("/usr/bob/1") shouldBe empty

    ///(Root / "user" / "bob").errorMessage(UserInfo) == Some("`details` not specified")

    //val router = new Router(Seq(Details, UserInfo))
    //val matchingRoutes = Seq[Route[_]] = router.filter(userInfo)
    //matchingRoutes == Seq(UserInfo)
  }

  "url()" should "work" in {
    case class UserInfo(user: String, details: Boolean)
    val userInfo = Root / "user" / Arg[String] / Arg[Boolean]
    val url = Router.url(userInfo, "hello" :: false :: HNil)
    assert(url == "/user/hello/false")
  }

  "url()" should "work on mapped route" in {
    case class UserInfo(user: String, details: Boolean)
    val userInfo = Root / "user" / Arg[String] / Arg[Boolean]
    val mapped   = Router.route[UserInfo](userInfo)
    val url      = Router.url(mapped, UserInfo("hello", false))
    assert(url == "/user/hello/false")
  }

  "fill()" should "work on mapped route" in {
    case class UserInfo(user: String, details: Boolean)
    val userInfo = Router.route[UserInfo](Root / "user" / Arg[String] / Arg[Boolean])

    val r = Router.fill(userInfo, UserInfo("hello", false))
    assert(r == Router.fill(userInfo.route, "hello" :: false :: HNil))
  }

  "parse()" should "work on mapped route" in {
    case class UserInfo(user: String, details: Boolean)
    val userInfo = Router.route[UserInfo](Root / "user" / Arg[String] / Arg[Boolean])
    assert(userInfo.parse("/user/hello/false")
      .contains(UserInfo("hello", false)))
  }

  "Composed route" should "just work" in {
    case class Details(userId: Int)
    case class UserInfo(user: String, details: Boolean)
    import Router.route

    val details  = route[Details](Root / "details" / Arg[Int])
    val userInfo = route[UserInfo](Root / "user" / Arg[String] / Arg[Boolean])

    val routes = ComposedRoute(details).orElse(userInfo)

    assert(routes.parse("/user/hello/false")
      .contains(UserInfo("hello", false)))
    assert(routes.parse("/details/42").contains(Details(42)))
  }
}
