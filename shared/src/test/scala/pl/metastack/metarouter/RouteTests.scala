package pl.metastack.metarouter

import org.scalatest._
import pl.metastack.metarouter._

class RouteTests extends FreeSpec with Matchers  {
  "A Route" - {
    "cannot equal InstantiatedRoute" in {
      val r1 = Root / "asdf"
      val r2 = (Root / "fdas").fill()
      assert(!r1.canEqual(r2), "r1 should not be comparable to r2")
    }
    "cannot equal a non-route" in {
      val r1 = Root / "asdf"
      assert(!r1.canEqual(2), "r1 should not be comparable to an integer")
      assert(!r1.canEqual("Asdf"), "r1 should not be comparable to a string")
      assert(r1 !== 2 )
    }
    "when empty" - {
      "should compile" in {
        val root = Root
      }
      "should equal the empty route" in {
        val root = Root
        val root2 = Root
        assert(root === root2)
      }
    }
    "when there are no Args" - {
      "should compile" in {
        val r = Root / "asdf"
      }
      "can compute its hashcode consistently" in {
        val r1 = Root / "asdf"
        val r2 = Root / "asdf"
        assert(r1.hashCode() === r1.hashCode())
        assert(r1.hashCode() === r2.hashCode())
      }
      "should equal an identical route" in {
        val foo = !# / "asdf" / "fdas"
        val bar = !# / "asdf" / "fdas"

        assert(foo === bar)
      }
      "should not equal a similiar route" in {
        val foo = !# / "asdf" / "fdas"
        val bar = !# / "asdf" / "fdas1"

        assert(foo !== bar)
      }
      "should not equal a longer route" in {
        val foo = !# / "asdf" / 1
        val bar = !# / "asdf" / 1 / "fdas"

        assert(foo !== bar)
      }
      "should not equal a longer shorter route" in {
        val bar = !# / "asdf" / 1 / "fdas"
        val foo = !# / "asdf" / 1

        assert(bar !== foo)
      }
    }
    "when there is one Arg" - {
      "should compile" in {
        val route = Root / "String" / Arg[Int]("Route")
      }
      "can compute its hashcode consistently" in {
        val r1 = Root / "String" / Arg[Int]("Route")
        val r2 = Root / "String" / Arg[Int]("Route")
        assert(r1.hashCode() === r1.hashCode())
        assert(r1.hashCode() === r2.hashCode())
      }
      "should compute the same hashcode as a similar route with different arg names" in {
        val r1 = Root / "String" / Arg[Int]("Route")
        val r2 = Root / "String" / Arg[Int]("Route2")
        assert(r1.hashCode() === r1.hashCode())
        assert(r1.hashCode() === r2.hashCode())
      }
      "should equal an identical route" in {
        val foo = !# / "asdf" / Arg[Int]("foo")
        val bar = !# / "asdf" / Arg[Int]("foo")

        assert(foo === bar)
      }
      "should equal a similar route with different arg names" in {
        val foo = !# / "asdf" / Arg[Int]("foo")
        val bar = !# / "asdf" / Arg[Int]("foo1")

        assert(foo === bar)
      }
      "should not equal another route with different arg names and types" in {
        val foo = !# / "asdf" / Arg[Int]("foo")
        val bar = !# / "asdf" / Arg[Boolean]("fdas")

        assert(foo !== bar)
      }
      "should not equal another route with different types but same names" in {
        val foo = !# / "asdf" / Arg[Int]("foo")
        val bar = !# / "asdf" / Arg[Boolean]("foo")

        assert(foo !== bar)
      }
    }
    "when there are multiple Args" - {
      "should compile" in {
        val r = Root / Arg[String]("db") / "asdf" / Arg[Int]("asf")
      }
      "should equal an identical route" in {
        val foo = !# / "asdf" / Arg[Int]("foo") / Arg[Boolean]("bar")
        val bar = !# / "asdf" / Arg[Int]("foo") / Arg[Boolean]("bar")

        assert(foo === bar)
      }
      "can compute its hashcode consistently" in {
        val r1 = !# / "asdf" / Arg[Int]("foo") / Arg[Boolean]("bar")
        val r2 = !# / "asdf" / Arg[Int]("foo") / Arg[Boolean]("bar")
        assert(r1.hashCode() === r1.hashCode())
        assert(r1.hashCode() === r2.hashCode())
      }
      "should compute the same hashcode as a similar route with different arg names" in {
        val foo = !# / "asdf" / Arg[Int]("foo") / Arg[Boolean]("bar")
        val bar = !# / "asdf" / Arg[Int]("foo1") / Arg[Boolean]("bar1")
        assert(foo.hashCode() === foo.hashCode())
        assert(foo.hashCode() === bar.hashCode())
      }
      "should equal a similar route with different arg names" in {
        val foo = !# / "asdf" / Arg[Int]("foo") / Arg[Boolean]("bar")
        val bar = !# / "asdf" / Arg[Int]("foo1") / Arg[Boolean]("bar1")

        assert(foo === bar)
      }
      "should not equal another route with different arg names and types" in {
        val foo = !# / "asdf" / Arg[Int]("foo") / Arg[Boolean]("bar")
        val bar = !# / "asdf" / Arg[Boolean]("fdas") / Arg[Int]("asdf")

        assert(foo !== bar)
      }
      "should not equal another route with different types but same names" in {
        val foo = !# / "asdf" / Arg[Int]("foo") / Arg[Boolean]("bar")
        val bar = !# / "asdf" / Arg[Boolean]("foo") / Arg[Int]("bar")

        assert(foo !== bar)
      }
    }
    "when using a custom path element" - {
      case class FooBar(foo: String)
      implicit object FooStaticElement extends StaticElement[FooBar] {
        def urlEncode(value: FooBar): String = value.foo
      }
      "should compile" in {
        val r = Root / FooBar("asdf")
      }
    }
    "when using a custom Arg element" - {
      case class FooBar(foo: String)
      implicit object FooParseableArg extends ParseableArg[FooBar] {
        def urlDecode(s: String): Option[FooBar] = Option(s).map(FooBar.apply)
        def urlEncode(s: FooBar): String = s.foo
      }
      "should compile" in {
        val r = Root / Arg[FooBar]("asdf")
      }
    }
  }
}