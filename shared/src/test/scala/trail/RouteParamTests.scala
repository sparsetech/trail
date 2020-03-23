package trail

import org.scalatest._

class RouteParamTests extends FunSpec with Matchers {
  import Route._

  it("Define route with one parameter") {
    val route = Root & Param[String]("test")
    assert(
      route == ParamRoute0(Root, Param[String]("test")))
  }

  it("Define non-root route with one parameter") {
    // TODO Due to operator precedence, we cannot use ? instead of & after strings
    val route = Root / "test" & Param[String]("test")
    assert(route.route == Root / "test")
    assert(
      route == ParamRoute0(
        ConcatRight(Root, Static("test")),
        Param[String]("test")))
  }

  it("Define route with two parameters") {
    // The ordering of parameters is not relevant for parsing, but is needed for
    // generation.
    val route = Root & Param[String]("test") & Param[Int]("test2")
    assert(route.route == ParamRoute0(Root, Param[String]("test")))
    assert(route.param == Param[Int]("test2"))
  }

  it("Define route with duplicated name") {
    // Duplicated names are not explicitly forbidden by RFC 3986 and used by
    // some applications.
    val route = Root & Param[String]("test") & Param[String]("test")
    assert(route.route == ParamRoute0(Root, Param[String]("test")))
    assert(route.param == Param[String]("test"))
    val url = route(("value", "value2"))
    assert(url == "/?test=value&test=value2")
  }

  it("Define route with duplicated name and different types") {
    val route = Root & Param[String]("test") & Param[Int]("test")
    assert(route.route == (Root & Param[String]("test")))
    assert(route.param == Param[Int]("test"))
    val url = route(("value", 42))
    assert(url == "/?test=value&test=42")
  }

  it("Generate URL of route with wrong parameter type") {
    """
    val route = Root & Param[Int]("test")
    val url = route("value")
    """ shouldNot typeCheck
  }

  it("Generate URL of route with one parameter") {
    val route = Root & Param[String]("test")
    val url = route("value")
    assert(url == "/?test=value")

    val url2 = route("äöü")
    assert(url2 == "/?test=%C3%A4%C3%B6%C3%BC")
  }

  it("Generate URL of route with two parameters") {
    val route = Root & Param[String]("test") & Param[Int]("test2")
    val url = route(("value", 42))
    assert(url == "/?test=value&test2=42")
  }

  it("Define URL of a route with optional parameter") {
    val route = Root & Param[Option[Int]]("test")

    val url = route(Option.empty[Int])
    assert(url == "/")

    val url2 = route(Option(42))
    assert(url2 == "/?test=42")

    val url3 = route(None)
    assert(url3 == "/")

    val url4 = route(Some(42))
    assert(url4 == "/?test=42")
  }

  it("Parsing route with one parameter") {
    val route = Root & Param[String]("test")
    assert(route.parseArgs("/?test=value")
      .contains("value"))
    assert(route.parseArgs("/?test=äöü").contains("äöü"))
    assert(route.parseArgs("/?test=%C3%A4%C3%B6%C3%BC")
      .contains("äöü"))
  }

  it("Parsing route with unspecified parameter") {
    val route = Root & Param[String]("test")
    assert(route.parseArgs("/").isEmpty)
    assert(route.parseArgs("/?test2=value").isEmpty)
  }

  it("Parsing route with two parameters") {
    val route = Root & Param[String]("test") & Param[Int]("test2")
    val parsed = route.parseArgs("/?test=value&test2=42")
    assert(parsed.contains(("value", 42)))
  }

  it("Parsing route with additional parameters") {
    // By default, allow parameters that are not specified in the route
    val route = Root & Param[String]("test")
    val parsed = route.parseArgs("/?test=value&test2=value2")
    assert(parsed.contains("value"))

    val route2 = Root & Params
    assert(route2.parseArgs("/?test=value").contains(() -> List("test" -> "value")))

    val route3 = Root & Param[String]("test") & Params
    assert(route3.parseArgs("/?test=value&test2=value2").contains("value" -> List("test2" -> "value2")))
  }

  it("Parsing non-root route with two optional parameters") {
    val route = Root / "register" & Param[Option[String]]("plan") & Param[Option[String]]("redirect")
    assert(route.parseArgs("/register?plan=test")
      .contains((Option("test"), Option.empty[String])))
    assert(route.parseArgs("/register?plan=test&redirect=test2")
      .contains((Option("test"), Option("test2"))))
    assert(route.parseArgs("/register?redirect=test2")
      .contains((Option.empty[String], Option("test2"))))
  }

  it("Parsing route with optional parameter") {
    val route = Root & Param[Option[String]]("test")

    assert(route.parseArgs("/").contains(Option.empty[String]))
    assert(route.parseArgs("/?test=value").contains(Option("value")))

    assert(route.parseArgs("/?test2=value").contains(None))
    assert(route.parseArgs("/?test=value&test2=value").contains(Some("value")))
  }

  it("Parsing route with duplicated name") {
    val route = Root & Param[String]("test") & Param[String]("test")
    assert(route.parseArgs("/?test=v1&test=v2")
      .contains(("v1", "v2")))
  }

  it("Parsing route with duplicated name and different types") {
    val route = Root & Param[Int]("test") & Param[String]("test")
    // When the two parameters have different types, the order matters
    assert(route.parseArgs("/?test=value&test=42").isEmpty)
    assert(route.parseArgs("/?test=42&test=value")
      .contains((42, "value")))
  }

  it("Only match parameter routes with same path") {
    val route = Root / "api" / "catalogue" / "content" & Param[String]("category")

    assert(route.parseArgs("/catalogue/content?category=Audio").isEmpty)
    assert(route.parseArgs("/api/catalogue/content?category=Audio")
      .contains("Audio"))
  }

  it("Parsing routes with additional parameters in strict mode") {
    // In strict mode, forbid parameters that are not specified in the route
    val route = Root & Param[String]("test")
    val parsed = route.parseArgsStrict("/?test=value&test2=value2")
    assert(parsed.isEmpty)

    val route2 = Root & Param[Option[String]]("test")
    assert(route2.parseArgsStrict("/").contains(Option.empty[String]))
    assert(route2.parseArgsStrict("/?test=value").contains(Option("value")))

    assert(route2.parseArgsStrict("/?test2=value").isEmpty)
    assert(route2.parseArgsStrict("/?test=value&test2=value").isEmpty)
  }
}
