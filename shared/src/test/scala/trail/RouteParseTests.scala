package trail

import org.scalatest.funspec.AnyFunSpec
import org.scalatest.matchers.should.Matchers

import scala.util.Try

class RouteParseTests extends AnyFunSpec with Matchers {
  it("Parse root") {
    val root = Root
    assert(root.parseArgs("/").contains(()))

    // Additional path elements cannot be specified
    assert(root.parseArgs("/test").isEmpty)
  }

  it("Parse root in strict mode") {
    val root = Root
    assert(root.parseArgsStrict("/").contains(()))
    assert(root.parseArgsStrict("/test").isEmpty)
    assert(root.parseArgsStrict("/?user=bob").isEmpty)
    assert(root.parseArgsStrict("/#test").isEmpty)
  }

  it("Parse one path element") {
    val root = Root / "test"
    assert(root.parseArgs("/").isEmpty)
    assert(root.parseArgs("/test").contains(()))
    assert(root.parseArgs("/test2").isEmpty)
  }

  it("Route with one argument can be parsed") {
    val route = Root / "user" / Arg[String]
    assert(route.parseArgs(trail.Path("/user/bob")) === Some("bob"))
    assert(route.parseArgs(trail.Path("/user/bob/test")).isEmpty)
  }

  it("Route can be parsed from Path") {
    val userInfo = Root / "user" / Arg[String] / Arg[Boolean]

    val parsed = userInfo.parseArgs(trail.Path("/user/bob/true"))
    assert(parsed === Some(("bob", true)))
    userInfo.parseArgs(trail.Path("/user/bob")) shouldBe empty
  }

  it("Route with query parameters can be parsed from Path") {
    val userInfo = Root / "user" / Arg[String] / Arg[Boolean] & Param[Int]("n")

    val parsed = userInfo.parseArgs(trail.Path("/user/bob/true", List("n" -> "42")))
    assert(parsed === Some((("bob", true), 42)))
  }

  it("Route with optional path element can be parsed") {
    val disk = Root / Arg[Option[Long]]
    assert(disk.parseArgs("/").contains(None))
    assert(disk.parseArgs("/42").contains(Some(42L)))
  }

  it("Route with optional path element can be parsed (2)") {
    val disk = Root / "disk" / Arg[Option[Long]]
    assert(disk.parseArgs("/disk").contains(None))
    assert(disk.parseArgs("/disk/").contains(None))
    assert(disk.parseArgs("/disk/42").contains(Some(42L)))
  }

  it("Route can be parsed from string") {
    val userInfo = Root / "user" / Arg[String] / Arg[Boolean]

    val parsed = userInfo.parseArgs("/user/bob/true")
    parsed shouldBe defined

    assert(parsed === Some(("bob", true)))

    // It is forbidden to provide more path elements than specified in the route
    userInfo.parseArgs("/user/bob/true/true") shouldBe empty

    userInfo.parseArgs("/user/bob") shouldBe empty
    userInfo.parseArgs("/user/bob/1") shouldBe empty
    userInfo.parseArgs("/usr/bob/1") shouldBe empty
  }

  it("Boolean argument can be parsed") {
    val userInfo = Root / "user" / Arg[String] / Arg[Boolean]

    assert(userInfo.parseArgs("/user/hello/false")
      .contains(("hello", false)))
  }

  it("Set codec") {
    implicit case object LongSetCodec extends Codec[Set[Long]] {
      override def encode(s: Set[Long]): Option[String] = Some(s.mkString(","))
      override def decode(s: Option[String]): Option[Set[Long]] =
        s.flatMap(value =>
          if (value.isEmpty) Some(Set())
          else Try(value.split(',').map(_.toLong).toSet).toOption)
    }

    val route = Root / "size" & Param[Set[Long]]("folders")
    assert(route.parseArgs(route(Set[Long]())).contains(Set[Long]()))
    assert(route.parseArgs(route(Set[Long](1))).contains(Set[Long](1)))
    assert(route.parseArgs(route(Set[Long](1, 2, 3))).contains(Set[Long](1, 2, 3)))
  }

  it("Match any path elements") {
    val route = Elems
    assert(route.parseArgs("/a/b/c").contains(List("a", "b", "c")))
  }

  it("Route with additional path elements") {
    val route = Root / Elems
    assert(route.parseArgs("/a/b/c").contains(List("a", "b", "c")))

    val route2 = Root / "a" / Elems
    assert(route2.parseArgs("/a/b/c").contains(List("b", "c")))

    val route3 = Root / "a" / Elems & Param[String]("test")
    assert(route3.parseArgs("/a/b/c?test=value").contains(
      List("b", "c") -> "value"))
  }
}
