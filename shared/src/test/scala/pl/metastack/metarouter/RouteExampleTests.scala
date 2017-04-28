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
    assert(Router.parse("/") === Root)
  }

  "A Modified Simple example" should "just work" in {
    val UserInfo  = Root / "user" / Arg[String] / Arg[Boolean]
    val userInfo  = Router.url(UserInfo, "bob" :: false :: HNil)
    val userInfo2 = Router.url(Root / "user" / "bob" / false, HNil)

    assert(userInfo === "/user/bob/false")
    assert(userInfo === userInfo2)

    assert(Router.parse("/a/b/c") === Root / "a" / "b" / "c")

    val parsed = Router.parse(UserInfo, "/user/bob/true")
    parsed shouldBe defined

    assert(parsed === Some("bob" :: true :: HNil))

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
    val userInfo = Root / "user" / Arg[String] / Arg[Boolean]
    val url      = Router.url(userInfo, "hello" :: false :: HNil)

    assert(url == "/user/hello/false")
  }

  "parse()" should "work" in {
    val userInfo = Root / "user" / Arg[String] / Arg[Boolean]

    assert(Router.parse(userInfo, "/user/hello/false")
      .contains("hello" :: false :: HNil))
  }

  "Composed route" should "work" in {
    val details  = Root / "details" / Arg[Int]
    val userInfo = Root / "user" / Arg[String] / Arg[Boolean]

    val routes = Router.create(details).orElse(userInfo)

    assert(routes.parse("/user/hello/false")
      .contains((userInfo, "hello" :: false :: HNil)))
    assert(routes.parse("/details/42").contains((details, 42 :: HNil)))
  }

  "Composed route" should "allow pattern matching" in {
    import shapeless._

    val details  = Root / "details" / Arg[Int]
    val userInfo = Root / "user" / Arg[String] / Arg[Boolean]

    val routes = Router.create(details).orElse(userInfo)

    val route = routes.parse("/user/hello/false").map {
      case (`details`, (a: Int) :: HNil) => a.toString
      case (`userInfo`, (u: String) :: (d: Boolean) :: HNil) => u + d
      case _ => ""
    }

    assert(route.contains("hellofalse"))
  }
}
