package trail

import org.scalatest._

class RouteParseTests extends FunSpec with Matchers {
  it("Parse root") {
    val root = Root
    assert(root.parse("/").contains(()))
  }

  it("Parse one path element") {
    val root = Root / "test"
    assert(root.parse("/").isEmpty)
    assert(root.parse("/test").contains(()))
    assert(root.parse("/test2").isEmpty)
  }

  it("Route with one argument can be parsed") {
    val route = Root / "user" / Arg[String]
    assert(route.parse(trail.Path("/user/bob")) === Some("bob"))
    assert(route.parse(trail.Path("/user/bob/test")) === Some("bob"))
  }

  it("Route can be parsed from Path") {
    val userInfo = Root / "user" / Arg[String] / Arg[Boolean]

    val parsed = userInfo.parse(trail.Path("/user/bob/true"))
    assert(parsed === Some(("bob", true)))
    userInfo.parse(trail.Path("/user/bob")) shouldBe empty
  }

  it("Route with query parameters can be parsed from Path") {
    val userInfo = Root / "user" / Arg[String] / Arg[Boolean] & Param[Int]("n")

    val parsed = userInfo.parse(trail.Path("/user/bob/true", List("n" -> "42")))
    assert(parsed === Some((("bob", true), 42)))
  }

  it("Route can be parsed from string") {
    val userInfo = Root / "user" / Arg[String] / Arg[Boolean]

    val parsed = userInfo.parse("/user/bob/true")
    parsed shouldBe defined

    assert(parsed === Some(("bob", true)))

    // It is possible to provide more path elements than specified in the route
    userInfo.parse("/user/bob/true/true") shouldBe defined

    userInfo.parse("/user/bob") shouldBe empty
    userInfo.parse("/user/bob/1") shouldBe empty
    userInfo.parse("/usr/bob/1") shouldBe empty
  }

  it("Boolean argument can be parsed") {
    val userInfo = Root / "user" / Arg[String] / Arg[Boolean]

    assert(userInfo.parse("/user/hello/false")
      .contains(("hello", false)))
  }
}
