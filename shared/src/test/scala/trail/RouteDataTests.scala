package trail

import org.scalatest._

class RouteDataTests extends WordSpec with Matchers {
  "A Route" when {
    "empty" should {
      "return root URL" in {
        val url = Root(())
        assert(url === "/")
      }
    }

    import Route._

    "no `Arg`s" should {
      "return a URL of the static path elements" in {
        val oneElement = Root / "asdf"
        assert(oneElement(()) === "/asdf")

        val twoElement = Root / "asdf" / "foo"
        assert(twoElement(()) === "/asdf/foo")

        val threeElement = Root / "asdf" / true / "foo"
        assert(threeElement(()) === "/asdf/true/foo")
      }
    }
    "one `Arg`" should {
      "return a URL of the static path elements with the args filled" in {
        val route0 = Root / Arg[Int]
        assert(route0(1) === "/1")

        val route = Root / "asdf" / Arg[Int]
        assert(route(1) === "/asdf/1")

        val route2 = Root / "asdf" / Arg[Int] / "true"
        assert(route2(1) === "/asdf/1/true")

        val route3 = Root / "asdf" / Arg[Int] / true
        assert(route3(1) === "/asdf/1/true")
      }
    }
    "multiple `Arg`s" should {
      "return a URL of the static path elements with the args filled" in {
        val r = Root / Arg[String] / "asdf" / Arg[Int]
        assert(r(("route", 1)) === "/route/asdf/1")
      }
    }
    "Long `Arg`" should {
      "return a URL of the static path elements with the args filled" in {
        val route = Root / Arg[Long]
        assert(route(600851475000L) === "/600851475000")
      }
    }
    "custom path element" should {
      case class Foo(bar: String)
      implicit object FooElement extends StaticElement[Foo](_.bar)

      "create URL" in {
        val r = Root / Foo("asdf")
        val i = r(())
        assert(i === "/asdf")
      }
    }
    "custom Arg element" should {
      case class Foo(bar: String)
      implicit object FooCodec extends Codec[Foo] {
        def encode(s: Foo): Option[String] = Some(s.bar)
        def decode(s: Option[String]): Option[Foo] = s.map(Foo)
      }
      "create URL" in {
        val r = Root / Arg[Foo]
        val i = r(Foo("dasd"))
        assert(i === "/dasd")
      }
    }
  }
}