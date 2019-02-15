package trail

import org.scalatest._

import shapeless.HNil

class RouteExampleTests extends FlatSpec with Matchers {
  "A simple example" should "just work" in {
    val userInfo     = Root / "user" / Arg[String] / Arg[Boolean]
    val userInfoUrl  = userInfo("bob" :: false :: HNil)
    val userInfo2    = (Root / "user" / "bob" / false).url(HNil)

    assert(userInfoUrl === "/user/bob/false")
    assert(userInfoUrl === userInfo2)

    val parsed = userInfo.parse("/user/bob/true")
    parsed shouldBe defined

    assert(parsed === Some("bob" :: true :: HNil))

    userInfo.parse("/user/bob") shouldBe empty
    userInfo.parse("/user/bob/true/true") shouldBe empty
    userInfo.parse("/user/bob/1") shouldBe empty
    userInfo.parse("/usr/bob/1") shouldBe empty
  }

  "url()" should "work" in {
    val userInfo = Root / "user" / Arg[String] / Arg[Boolean]
    val url      = userInfo("hello" :: false :: HNil)

    assert(url == "/user/hello/false")
  }

  "url()" should "work with multiple optional parameters" in {
    val list = Root / "list" & ParamOpt[Int]("num") & ParamOpt[Boolean]("upload")
    val url  = list(HNil, Option.empty[Int] :: Option(true) :: HNil)

    assert(url == "/list?upload=true")
  }

  "parse()" should "work" in {
    val userInfo = Root / "user" / Arg[String] / Arg[Boolean]

    assert(userInfo.parse("/user/hello/false")
      .contains("hello" :: false :: HNil))
  }

  "Expressing a routing table" should "be possible with pattern matching" in {
    import shapeless._

    val details  = Root / "details" / Arg[Int]
    val userInfo = Root / "user" / Arg[String] / Arg[Boolean]

    val result = "/user/hello/false" match {
      case details (a :: HNil)      => a.toString
      case userInfo(u :: d :: HNil) =>
        val user: String = u  // Verify that type inference works
        u + d
    }

    assert(result == "hellofalse")
  }

  "Expressing a routing table" should "be possible with pattern matching (2)" in {
    import shapeless._

    val details  = Root / "details" / Arg[Int]
    val userInfo = Root / "user" / Arg[String] / Arg[Boolean]

    val result = PathParser.parse("/user/hello/false") match {
      case details (a :: HNil)      => a.toString
      case userInfo(u :: d :: HNil) =>
        val user: String = u  // Verify that type inference works
        u + d
    }

    assert(result == "hellofalse")
  }
}
