package pl.metastack.metarouter

import org.scalatest._

class InstantiatedRouteTests extends WordSpec with Matchers  {
  "A Route" when {
    "empty" should {
      "return root url" in {
        assert(Root.fill().url === "/")
      }
    }
    "no Args" should {
      "return a url of the static path elements" in {
        val oneElement = Root / "asdf"
        assert(oneElement.fill().url === "/asdf")

        val twoElement = Root / "asdf" / "foo"
        assert(twoElement.fill().url === "/asdf/foo")

        val threeElement = Root / "asdf" / true / "foo"
        assert(threeElement.fill().url === "/asdf/true/foo")
      }
    }
    "one Arg" should {
      "return a url of the static path elements with the args filled" in {
        val route = Root / "asdf" / Arg[Int]("asf")
        assert(route.fill(1).url === "/asdf/1")

        val route2 = Root / "asdf" / Arg[Int]("asf") / true
        assert(route2.fill(1).url === "/asdf/1/true")
      }
    }
    "multiple Args" should {
      "return a url of the static path elements with the args filled" in {
        val r = Root / Arg[String]("db") / "asdf" / Arg[Int]("asf")
        assert(r.fill("route", 1).url === "/route/asdf/1")
      }
    }
    "custom path element" should {
      case class FooBar(foo: String)
      implicit object FooStaticElement extends StaticElement[FooBar] {
        def urlEncode(value: FooBar): String = value.foo
      }
      "create url" in {
        val r = Root / FooBar("asdf")
        val i = r.fill()
        assert(i.url === "/asdf")
      }
    }
    "custom Arg element" should {
      case class FooBar(foo: String)
      implicit object FooParseableArg extends ParseableArg[FooBar] {
        def urlDecode(s: String): Option[FooBar] = Option(s).map(FooBar.apply)
        def urlEncode(s: FooBar): String = s.foo
      }
      "create url" in {
        val r = Root / Arg[FooBar]("asdf")
        val i = r.fill(FooBar("dasd"))
        assert(i.url === "/dasd")
      }
    }
  }
}