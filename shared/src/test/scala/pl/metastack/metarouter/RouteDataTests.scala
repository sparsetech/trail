package pl.metastack.metarouter

import org.scalatest._
import shapeless.HNil

class RouteDataTests extends WordSpec with Matchers {
  "A Route" when {
    "empty" should {
      "return root URL" in {
        val url = Root(HNil)
        assert(url === "/")
      }
    }
    "no `Arg`s" should {
      "return a URL of the static path elements" in {
        val oneElement = Root / "asdf"
        assert(oneElement(HNil) === "/asdf")

        val twoElement = Root / "asdf" / "foo"
        assert(twoElement(HNil) === "/asdf/foo")

        val threeElement = Root / "asdf" / true / "foo"
        assert(threeElement(HNil) === "/asdf/true/foo")
      }
    }
    "one `Arg`" should {
      "return a URL of the static path elements with the args filled" in {
        val route = Root / "asdf" / Arg[Int]
        assert(route(1 :: HNil) === "/asdf/1")

        val route2 = Root / "asdf" / Arg[Int] / true
        assert(route2(1 :: HNil) === "/asdf/1/true")
      }
    }
    "multiple `Arg`s" should {
      "return a URL of the static path elements with the args filled" in {
        val r = Root / Arg[String] / "asdf" / Arg[Int]
        assert(r("route" :: 1 :: HNil) === "/route/asdf/1")
      }
    }
    "Long `Arg`" should {
      "return a URL of the static path elements with the args filled" in {
        val route = Root / Arg[Long]
        assert(route(600851475000L :: HNil) === "/600851475000")
      }
    }
    "custom path element" should {
      case class Foo(bar: String)
      implicit object FooElement extends StaticElement[Foo](_.bar)

      "create URL" in {
        val r = Root / Foo("asdf")
        val i = r(HNil)
        assert(i === "/asdf")
      }
    }
    "custom Arg element" should {
      case class Foo(bar: String)
      implicit object FooCodec extends Codec[Foo] {
        def encode(s: Foo): String = s.bar
        def decode(s: String): Option[Foo] = Option(s).map(Foo)
      }
      "create URL" in {
        val r = Root / Arg[Foo]
        val i = r(Foo("dasd") :: HNil)
        assert(i === "/dasd")
      }
    }
  }
}