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

  "A Modified Simple example" should "just work" in {
    val Details  = Root / "details" / Arg[Int]
    val UserInfo = Root / "user" / Arg[String] / Arg[Boolean]

    val userInfo = UserInfo.fillN("bob", false)

    assert(userInfo.url === "/user/bob/false")
    assert(userInfo.route === UserInfo)

    assert(userInfo === (Root / "user" / "bob" / false).fill())

    assert(Route.parse("/a/b/c") === (Root / "a" / "b" / "c").fill())

    assert(UserInfo.matches("/user/bob/true").isRight)
    val parsedRoute = UserInfo.matches("/user/bob/true").right.get
    assert(parsedRoute.route === UserInfo)
    assert(parsedRoute.data == "bob" :: true :: HNil)

    val i1 = UserInfo.matches("/user/bob")
    assert(i1.isLeft)
    assert(i1.left.get === "Path is too short.")

    val i2 = UserInfo.matches("/user/bob/true/true")
    assert(i2.isLeft)
    assert(i2.left.get === "Path is too long.")

    val i3 = UserInfo.matches("/user/bob/1")
    assert(i3.isLeft)
    assert(i3.left.get === "Argument `1` could not be parsed")

    val i4 = UserInfo.matches("/usr/bob/1")
    assert(i4.isLeft)
    assert(i4.left.get === "Path element `user` did not match `usr`")

    ///(Root / "user" / "bob").errorMessage(UserInfo) == Some("`details` not specified")

    //val router = new Router(Seq(Details, UserInfo))
    //val matchingRoutes = Seq[Route[_]] = router.filter(userInfo)
    //matchingRoutes == Seq(UserInfo)
  }

  "Mapped route" should "just work" in {
    case class UserInfo(user: String, details: Boolean)
    val userInfo = (Root / "user" / Arg[String] / Arg[Boolean]).as[UserInfo]

    val r = userInfo(UserInfo("hello", false))
    assert(r == userInfo.route.fillN("hello", false))

    assert(userInfo.parse("/user/hello/false")
      .contains(UserInfo("hello", false)))
  }

  "Composed route" should "just work" in {
    case class Details(userId: Int)
    case class UserInfo(user: String, details: Boolean)

    val details  = (Root / "details" / Arg[Int]).as[Details]
    val userInfo = (Root / "user" / Arg[String] / Arg[Boolean]).as[UserInfo]

    val routes = ComposedRoute(details).orElse(userInfo)

    assert(routes.parse("/user/hello/false")
      .contains(UserInfo("hello", false)))
    assert(routes.parse("/details/42").contains(Details(42)))
  }
}
