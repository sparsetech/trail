package trail

import org.scalatest._
import trail.Route.ParamRoute0

class RouteFragmentTests extends FunSpec with Matchers {
  it("Define route with fragment") {
    val route = Root $ Fragment[String]
    assert(route.route == Root)
    assert(route.fragment == Fragment[String])
  }

  it("Define non-root route with fragment") {
    val route = Root / "test" $ Fragment[String]
    assert(route.route == Root / "test")
    assert(route.fragment == Fragment[String])
  }

  it("Define route with parameter and fragment") {
    val route = Root & Param[String]("test") $ Fragment[Int]
    assert(route.route == ParamRoute0(Root, Param[String]("test")))
    assert(route.fragment == Fragment[Int])
  }

  it("Parse route with empty fragment") {
    val route = Root $ Fragment[String]
    assert(route.parse("/").isEmpty)
    assert(route.parse("/#").contains(""))
  }

  it("Generate URL for route with empty fragment") {
    val route = Root $ Fragment[String]
    val url = route("")
    assert(url == "/#")
  }

  it("Parse route with optional fragment") {
    val route = Root $ Fragment[Option[String]]
    assert(route.parse("/").contains(None))
    assert(route.parse("/#").contains(Some("")))
  }

  it("Generate URL for route with optional fragment") {
    val route = Root $ Fragment[Option[String]]
    val url = route(None)
    assert(url == "/")
  }

  it("Generate URL for route with fragment") {
    val route = Root $ Fragment[Int]
    val url = route(42)
    assert(url == "/#42")
  }

  it("Generate URL for route with parameter and fragment") {
    val route = Root & Param[String]("test") $ Fragment[Int]
    val url = route(("value", 42))
    assert(url == "/?test=value#42")
  }

  it("Parse route with fragment") {
    val route = Root $ Fragment[String]
    assert(route.parse("/").isEmpty)
    assert(route.parse("/#value").contains("value"))
  }

  it("Parse route with parameter and fragment") {
    val route = Root & Param[String]("test") $ Fragment[Int]
    assert(route.parse("/?test=value#42").contains(("value", 42)))
  }
}
