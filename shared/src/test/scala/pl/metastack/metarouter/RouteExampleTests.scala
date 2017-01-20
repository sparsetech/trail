package pl.metastack.metarouter

import org.scalatest._

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
    Router.parse(Root, "/") shouldBe defined
  }

  "A Modified Simple example" should "just work" in {
    val UserInfo  = Root / "user" / Arg[String] / Arg[Boolean]
    val userInfo  = Router.fill(UserInfo, "bob" :: false :: HNil)
    val userInfo2 = Router.fill(Root / "user" / "bob" / false)

    assert(Router.url(userInfo) === "/user/bob/false")
    assert(userInfo.route === UserInfo)

    assert(Router.url(userInfo) === Router.url(userInfo2))

    assert(Router.parse("/a/b/c") === Router.fill(Root / "a" / "b" / "c"))

    Router.parse(UserInfo, "/user/bob/true") shouldBe defined
    val parsedRoute = Router.parse(UserInfo, "/user/bob/true").get
    assert(parsedRoute.route === UserInfo)
    assert(parsedRoute.data == "bob" :: true :: HNil)

    Router.parse(UserInfo, "/user/bob") shouldBe empty
    Router.parse(UserInfo, "/user/bob/true/true") shouldBe empty
    Router.parse(UserInfo, "/user/bob/1") shouldBe empty
    Router.parse(UserInfo, "/usr/bob/1") shouldBe empty

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
    assert(Router.parse(userInfo, "/user/hello/false")
      .contains(UserInfo("hello", false)))
  }

  "Composed route" should "just work" in {
    case class Details(userId: Int)
    case class UserInfo(user: String, details: Boolean)
    import Router.route

    val details  = route[Details](Root / "details" / Arg[Int])
    val userInfo = route[UserInfo](Root / "user" / Arg[String] / Arg[Boolean])

    val routes = Router.create(details).orElse(userInfo)

    assert(routes.parse("/user/hello/false")
      .contains(UserInfo("hello", false)))
    assert(routes.parse("/details/42").contains(Details(42)))
  }
}
