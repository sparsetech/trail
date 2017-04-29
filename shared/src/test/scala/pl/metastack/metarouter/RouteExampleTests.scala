package pl.metastack.metarouter

import org.scalatest._

import shapeless.HNil

class RouteExampleTests extends FlatSpec with Matchers {
  "A simple example" should "just work" in {
    val UserInfo  = Root / "user" / Arg[String] / Arg[Boolean]
    val userInfo  = Router.url(UserInfo, "bob" :: false :: HNil)
    val userInfo2 = Router.url(Root / "user" / "bob" / false, HNil)

    assert(userInfo === "/user/bob/false")
    assert(userInfo === userInfo2)

    val parsed = Router.parse(UserInfo, "/user/bob/true")
    parsed shouldBe defined

    assert(parsed === Some("bob" :: true :: HNil))

    Router.parse(UserInfo, "/user/bob") shouldBe empty
    Router.parse(UserInfo, "/user/bob/true/true") shouldBe empty
    Router.parse(UserInfo, "/user/bob/1") shouldBe empty
    Router.parse(UserInfo, "/usr/bob/1") shouldBe empty
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

  "Expressing a routing table" should "be possible with pattern matching" in {
    import shapeless._

    val details  = Root / "details" / Arg[Int]
    val userInfo = Root / "user" / Arg[String] / Arg[Boolean]

    val result = "/user/hello/false" match {
      case `details` (a :: HNil)      => a.toString
      case `userInfo`(u :: d :: HNil) =>
        val user: String = u  // Verify that type inference works
        u + d
    }

    assert(result == "hellofalse")
  }
}
