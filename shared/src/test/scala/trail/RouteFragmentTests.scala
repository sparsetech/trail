package trail

import org.scalatest._
import shapeless._

class RouteFragmentTests extends FunSpec with Matchers {
  it("Define route with fragment") {
    val route = Root & Fragment[String]
    assert(route.route == Root)
    assert(route.params == Fragment[String] :: HNil)
  }

  it("Define non-root route with fragment") {
    val route = Root / "test" & Fragment[String]
    assert(route.route == Root / "test")
    assert(route.params == Fragment[String] :: HNil)
  }

  it("Define route with parameter and fragment") {
    val route = Root & Param[String]("test") & Fragment[Int]
    assert(route.route == Root)
    assert(route.params == Param[String]("test") :: Fragment[Int] :: HNil)
  }

  it("Generate URL for route with fragment") {
    val route = Root & Fragment[Int]
    val url = route(HNil, 42 :: HNil)
    assert(url == "/#42")
  }

  it("Generate URL for route with parameter and fragment") {
    val route = Root & Param[String]("test") & Fragment[Int]
    val url = route(HNil, "value" :: 42 :: HNil)
    assert(url == "/?test=value#42")
  }

  it("Parse route with fragment") {
    val route = Root & Fragment[String]
    assert(route.parse("/").isEmpty)
    assert(route.parse("/#value").contains((HNil, "value" :: HNil)))
  }

  it("Parse route with parameter and fragment") {
    val route = Root & Param[String]("test") & Fragment[Int]
    assert(route.parse("/?test=value#42").contains(
      (HNil, "value" :: 42 :: HNil)))
  }
}
