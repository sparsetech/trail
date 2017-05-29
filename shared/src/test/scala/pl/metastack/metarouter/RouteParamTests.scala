package pl.metastack.metarouter

import org.scalatest._
import shapeless._

class RouteParamTests extends FunSpec with Matchers {
  it("Define route with one parameter") {
    val route = Root & Param[String]("test")
    assert(route.route == Root)
    assert(route.params == Param[String]("test") :: HNil)
  }

  it("Define non-root route with one parameter") {
    // TODO Due to operator precedence, we cannot use ? instead of & after strings
    val route = Root / "test" & Param[String]("test")
    assert(route.route == Root / "test")
    assert(route.params == Param[String]("test") :: HNil)
  }

  it("Define route with two parameters") {
    // The ordering of parameters is not relevant for parsing, but is needed for
    // generation.
    val route = Root & Param[String]("test") & Param[Int]("test2")
    assert(route.route == Root)
    assert(route.params == Param[String]("test") :: Param[Int]("test2") :: HNil)
  }

  it("Define route with duplicated name") {
    // Duplicated names are not explicitly forbidden by RFC 3986 and used by
    // some applications.
    val route = Root & Param[String]("test") & Param[String]("test")
    assert(route.route == Root)
    assert(route.params == Param[String]("test") :: Param[String]("test") :: HNil)
    val url = route(HNil, "value" :: "value2" :: HNil)
    assert(url == "/?test=value&test=value2")
  }

  it("Define route with duplicated name and different types") {
    val route = Root & Param[String]("test") & Param[Int]("test")
    assert(route.route == Root)
    assert(route.params == Param[String]("test") :: Param[Int]("test") :: HNil)
    val url = route(HNil, "value" :: 42 :: HNil)
    assert(url == "/?test=value&test=42")
  }

  it("Generate URL of route with wrong parameter type") {
    """
    val route = Root & Param[Int]("test")
    val url = route(HNil, "value" :: HNil)
    """ shouldNot typeCheck
  }

  it("Generate URL of route with one parameter") {
    val route = Root & Param[String]("test")
    val url = route(HNil, "value" :: HNil)
    assert(url == "/?test=value")

    val url2 = route(HNil, "äöü" :: HNil)
    assert(url2 == "/?test=%C3%A4%C3%B6%C3%BC")
  }

  it("Generate URL of route with two parameters") {
    val route = Root & Param[String]("test") & Param[Int]("test2")
    val url = route(HNil, "value" :: 42 :: HNil)
    assert(url == "/?test=value&test2=42")
  }

  it("Define URL of a route with optional parameter") {
    val route = Root & ParamOpt[Int]("test")

    val url = route(HNil, Option.empty[Int] :: HNil)
    assert(url == "/")

    val url2 = route(HNil, Option(42) :: HNil)
    assert(url2 == "/?test=42")

    // TODO The following does not work
    // val url3 = route(HNil, None :: HNil)
    // assert(url3 == "/")

    // val url4 = route(HNil, Some(42) :: HNil)
    // assert(url4 == "/?test=42")
  }

  it("Parsing route with one parameter") {
    val route = Root & Param[String]("test")
    assert(route.parse("/?test=value")
      .contains((HNil, "value" :: HNil)))
    assert(route.parse("/?test=äöü").contains((HNil, "äöü" :: HNil)))
    assert(route.parse("/?test=%C3%A4%C3%B6%C3%BC")
      .contains((HNil, "äöü" :: HNil)))
  }

  it("Parsing route with unspecified parameter") {
    val route = Root & Param[String]("test")
    assert(route.parse("/").isEmpty)
    assert(route.parse("/?test2=value").isEmpty)
  }

  it("Parsing route with two parameters") {
    val route = Root & Param[String]("test") & Param[Int]("test2")
    val parsed = route.parse("/?test=value&test2=42")
    assert(parsed.contains((HNil, "value" :: 42 :: HNil)))
  }

  it("Parsing route with additional parameters") {
    // Ignore parameters that are not specified in the route
    val route = Root & Param[String]("test")
    val parsed = route.parse("/?test=value&test2=value2")
    assert(parsed.contains((HNil, "value" :: HNil)))
  }

  it("Parsing non-root route with two optional parameters") {
    val route = Root / "register" & ParamOpt[String]("plan") & ParamOpt[String]("redirect")
    assert(route.parse("/register?plan=test")
      .contains((HNil, Option("test") :: Option.empty[String] :: HNil)))
    assert(route.parse("/register?plan=test&redirect=test2")
      .contains((HNil, Option("test") :: Option("test2") :: HNil)))
    assert(route.parse("/register?redirect=test2")
      .contains((HNil, Option.empty[String] :: Option("test2") :: HNil)))
  }

  it("Parsing route with optional parameter") {
    val route = Root & ParamOpt[String]("test")
    assert(route.parse("/")
      .contains((HNil, Option.empty[String] :: HNil)))
    assert(route.parse("/?test2=value")
      .contains((HNil, Option.empty[String] :: HNil)))
    assert(route.parse("/?test=value")
      .contains((HNil, Option("value") :: HNil)))
    assert(route.parse("/?test=value&test2=value")
      .contains((HNil, Option("value") :: HNil)))
  }

  it("Parsing route with duplicated name") {
    val route = Root & Param[String]("test") & Param[String]("test")
    assert(route.parse("/?test=v1&test=v2")
      .contains((HNil, "v1" :: "v2" :: HNil)))
  }

  it("Parsing route with duplicated name and different types") {
    val route = Root & Param[Int]("test") & Param[String]("test")
    // When the two parameters have different types, the order matters
    assert(route.parse("/?test=value&test=42").isEmpty)
    assert(route.parse("/?test=42&test=value")
      .contains((HNil, 42 :: "value" :: HNil)))
  }

  it("Parsing multiple route with pattern matching") {
    val route  = Root & Param[Int]("test")
    val route2 = Root & ParamOpt[String]("test2")

    val result = "/?test2=value" match {
      case route (HNil, a :: HNil) => a
      case route2(HNil, b :: HNil) => b
    }

    assert(result == Some("value"))
  }
}
