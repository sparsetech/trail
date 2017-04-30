package pl.metastack.metarouter

import org.scalatest._
import shapeless.HNil

class UrlTests extends WordSpec with Matchers  {
  "A Route" when {
    "empty" should {
      "create InstantiatedRoute" in {
        Root(HNil)
      }
      "not compile InstantiatedRoute with args" in {
        "Root(1 :: HNil)" shouldNot typeCheck
        """Root("asdf" :: HNil)""" shouldNot typeCheck
      }
    }
    "no Args" should {
      "create InstantiatedRoute" in {
        val r = Root / "asdf"
        r(HNil)
      }
      "not compile InstantiatedRoute with args" in {
        """
        val r = Root / "asdf"
        r(1 :: HNil)
        """ shouldNot typeCheck
        """
        val r = Root / "asdf"
        r("asdf" :: HNil)
        """.stripMargin shouldNot typeCheck
      }
    }
    "one Arg" should {
      "create URL" in {
        val route = Root / "asdf" / Arg[Int]
        route(1 :: HNil)
      }
      "not compile with InstantiatedRoute with invalid arg type" in {
        """
        val r = Root / "asdf" / Arg[Int]
        r("Route" :: HNil)
        """ shouldNot typeCheck
      }
      "not compile with InstantiatedRoute with invalid arg number" in {
        """
        val r = Root / "asdf" / Arg[Int]
        r("Route" :: 1 :: HNil)
        """ shouldNot typeCheck
      }
    }
    "multiple Args" should {
      "create URL" in {
        val r = Root / Arg[String] / "asdf" / Arg[Int]
        r("Route" :: 1 :: HNil)
      }
      "not compile with wrong argument order" in {
        """
        val r = Root / Arg[String] / "asdf" / Arg[Int]
        r(1 :: "Route" :: HNil)
        """ shouldNot typeCheck
      }
      "not compile with wrong argument number" in {
        """
        val r = Root / Arg[String] / "asdf" / Arg[Int]
        r(1 :: 1 :: 1 :: HNil)
        """ shouldNot typeCheck
        """
        val r = Root / Arg[String] / "asdf" / Arg[Int]
        r(1 :: HNil)
        """ shouldNot typeCheck
        """
        val r = Root / Arg[String] / "asdf" / Arg[Int]
        r("Route" :: 1 :: 1 :: HNil)
        """ shouldNot typeCheck
      }
    }
    "custom path element" should {
      case class FooBar(foo: String)
      implicit object FooStaticElement extends StaticElement[FooBar] {
        def urlEncode(value: FooBar): String = value.foo
      }
      "create URL" in {
        val r = Root / FooBar("asdf")
        assert(r(HNil) == "/asdf")
      }
      "not compile InstantiatedRoute with args" in {
        case class NoGood(bar: String)
        """
        val r = Root / NoGood("asdf")
        """ shouldNot typeCheck
      }
    }
    "custom Arg element" should {
      case class FooBar(foo: String)
      implicit object FooParseableArg extends ParseableArg[FooBar] {
        override def urlDecode(s: String) = Option(s).map(FooBar.apply)
        override def urlEncode(s: FooBar) = s.foo
      }
      "create URL" in {
        val r = Root / Arg[FooBar]
        assert(r(FooBar("dasd") :: HNil) == "/dasd")
      }
      "not compile InstantiatedRoute with args" in {
        case class NoGood(bar: String)
        """
        val r = Root / NoGood("asdf")
        """ shouldNot typeCheck
      }
    }
  }
}