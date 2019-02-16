package trail

import org.scalatest._

import shapeless.HNil

class RouteExampleTests extends FlatSpec with Matchers {
  "url()" should "work with mandatory arguments" in {
    val userInfo  = Root / "user" / Arg[String] / Arg[Boolean]

    val url1 = userInfo("bob" :: false :: HNil)
    val url2 = (Root / "user" / "bob" / false).url(HNil)

    assert(url1 === "/user/bob/false")
    assert(url2 === "/user/bob/false")
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
