package trail

import org.scalatest._

class RoutingTableTests extends FunSpec with Matchers {
  it("Express a routing table with pattern matching") {
    val details  = Root / "details" / Arg[Int]
    val userInfo = Root / "user" / Arg[String] / Arg[Boolean]

    val result = "/user/hello/false" match {
      case details (a)      => a.toString
      case userInfo((u, d)) =>
        val user: String = u  // Verify that type inference works
        u + d
    }

    assert(result == "hellofalse")
  }

  it("Express a routing table with pattern matching (2)") {
    val details  = Root / "details" / Arg[Int]
    val userInfo = Root / "user" / Arg[String] / Arg[Boolean]

    val result = PathParser.parse("/user/hello/false") match {
      case details (a)      => a.toString
      case userInfo((u, d)) =>
        val user: String = u  // Verify that type inference works
        u + d
    }

    assert(result == "hellofalse")
  }

  it("Express conditional routes") {
    class Routes(isProduction: Boolean) {
      val root  = if (isProduction) Root / "api" / "v2.0" else Root
      val users = root / "users" / Arg[String]
    }

    val devRoutes  = new Routes(isProduction = false)
    val prodRoutes = new Routes(isProduction = true)

    assert(devRoutes.users.parseArgs("/users/test").contains("test"))
    assert(prodRoutes.users.parseArgs("/api/v2.0/users/test").contains("test"))
  }

  it("Parsing multiple route with pattern matching") {
    val route  = Root & Param[Int]("test")
    val route2 = Root & Param[Option[String]]("test2")

    val result = "/?test2=value" match {
      case route (a) => a
      case route2(b) => b
    }

    assert(result == Some("value"))
  }
}
