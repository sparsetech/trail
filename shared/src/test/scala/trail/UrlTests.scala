package trail

import org.scalatest.funspec.AnyFunSpec
import org.scalatest.wordspec.AnyWordSpec
import org.scalatest.matchers.should.Matchers

class UrlTests extends AnyWordSpec with Matchers {
  "A Route" when {
    "empty" should {
      "create root" in {
        Root
      }
      // TODO Int and String to Unit are valid conversions
//      "not compile root with args" in {
//        "Root(1)" shouldNot typeCheck
//        """Root("asdf")""" shouldNot typeCheck
//      }
    }
    "no Args" should {
      "create URL" in {
        val r = Root / "asdf"
        r(())
      }
//      "not compile route with wrong parameter" in {
//        """
//        val r = Root / "asdf"
//        r(1)
//        """ shouldNot typeCheck
//        """
//        val r = Root / "asdf"
//        r("asdf")
//        """.stripMargin shouldNot typeCheck
//      }
    }
    "one Arg" should {
      "create URL" in {
        val route = Root / "asdf" / Arg[Int]
        route(1)
      }
      "not compile with InstantiatedRoute with invalid arg type" in {
        """
        val r = Root / "asdf" / Arg[Int]
        r("Route")
        """ shouldNot typeCheck
      }
      "not compile with InstantiatedRoute with invalid arg number" in {
        """
        val r = Root / "asdf" / Arg[Int]
        r(("Route", 1))
        """ shouldNot typeCheck
      }
    }
    "multiple Args" should {
      "create URL" in {
        val r = Root / Arg[String] / "asdf" / Arg[Int]
        r(("Route", 1))
      }
      "not compile with wrong argument order" in {
        """
        val r = Root / Arg[String] / "asdf" / Arg[Int]
        r((1, "Route"))
        """ shouldNot typeCheck
      }
      "not compile with wrong argument number" in {
        """
        val r = Root / Arg[String] / "asdf" / Arg[Int]
        r(((1, 1), 1))
        """ shouldNot typeCheck
        """
        val r = Root / Arg[String] / "asdf" / Arg[Int]
        r(1)
        """ shouldNot typeCheck
        """
        val r = Root / Arg[String] / "asdf" / Arg[Int]
        r((("Route", 1), 1))
        """ shouldNot typeCheck
      }
    }
    "custom path element" should {
      case class Foo(bar: String)
      implicit object FooElement extends StaticElement[Foo](_.bar)
      "create URL" in {
        val r = Root / Foo("asdf")
        assert(r(()) == "/asdf")
      }
      "not compile InstantiatedRoute with args" in {
        case class NoGood(bar: String)
        """
        val r = Root / NoGood("asdf")
        """ shouldNot typeCheck
      }
    }
    "custom Arg element" should {
      case class Foo(foo: String)
      implicit object FooCodec extends Codec[Foo] {
        override def encode(s: Foo): Option[String] = Some(s.foo)
        override def decode(s: Option[String]): Option[Foo] = s.map(Foo)
      }
      "create URL" in {
        val r = Root / Arg[Foo]
        assert(r(Foo("dasd")) == "/dasd")
      }
      "not compile route with missing codec" in {
        case class NoGood(bar: String)
        """
        val r = Root / NoGood("asdf")
        """ shouldNot typeCheck
      }
    }
  }
}

class UrlTests2 extends AnyFunSpec {
  it("url() work with mandatory arguments") {
    val userInfo  = Root / "user" / Arg[String] / Arg[Boolean]

    val url1 = userInfo(("bob", false))
    val url2 = (Root / "user" / "bob" / false).url(())

    assert(url1 === "/user/bob/false")
    assert(url2 === "/user/bob/false")
  }

  it("url() should work") {
    val userInfo = Root / "user" / Arg[String] / Arg[Boolean]
    val url      = userInfo(("hello", false))

    assert(url == "/user/hello/false")
  }

  it("url() should work with multiple optional parameters") {
    val list = Root / "list" & Param[Option[Int]]("num") & Param[Option[Boolean]]("upload")
    val url  = list((Option.empty[Int], Option(true)))

    assert(url == "/list?upload=true")
  }

  it("url() should work with optional parameter set to None") {
    val route = Root / "disk" / "view" & Param[Long]("disk") & Param[Option[Long]]("folder")
    val url   = route((48L, None))

    assert(url == "/disk/view?disk=48")
  }

  it("url() should work with additional parameters") {
    val route = Root & Params
    assert(route.url(() -> List("test" -> "value")) == "/?test=value")

    val route2 = Root & Param[String]("test") & Params
    assert(route2.url("value" -> List("test2" -> "value2")) == "/?test=value&test2=value2")

    val route3 = Root & Params $ Fragment[Int]
    assert(route3.url(() -> List("test" -> "value") -> 42) == "/?test=value#42")
  }
}
