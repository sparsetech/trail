package trail

import org.scalatest._
import shapeless._

class RouteParseTests extends FunSpec with Matchers {
  it("Route can be parsed from Path") {
    val userInfo = Root / "user" / Arg[String] / Arg[Boolean]

    val parsed = userInfo.parse(trail.Path("/user/bob/true"))
    assert(parsed === Some("bob" :: true :: HNil))
    userInfo.parse(trail.Path("/user/bob")) shouldBe empty
  }

  it("Route with query parameters can be parsed from Path") {
    val userInfo = Root / "user" / Arg[String] / Arg[Boolean] & Param[Int]("n")

    val parsed = userInfo.parse(trail.Path("/user/bob/true", List("n" -> "42")))
    assert(parsed === Some("bob" :: true :: HNil, 42 :: HNil))
  }

  it("Route can be parsed from string") {
    val userInfo = Root / "user" / Arg[String] / Arg[Boolean]

    val parsed = userInfo.parse("/user/bob/true")
    parsed shouldBe defined

    assert(parsed === Some("bob" :: true :: HNil))

    userInfo.parse("/user/bob") shouldBe empty
    userInfo.parse("/user/bob/true/true") shouldBe empty
    userInfo.parse("/user/bob/1") shouldBe empty
    userInfo.parse("/usr/bob/1") shouldBe empty
  }

  it("Boolean argument can be parsed") {
    val userInfo = Root / "user" / Arg[String] / Arg[Boolean]

    assert(userInfo.parse("/user/hello/false")
      .contains("hello" :: false :: HNil))
  }
}
