package pl.metastack.metarouter

import org.scalatest._
import shapeless.HNil

class UrlTests extends WordSpec with Matchers  {
  "A Route" when {
    "empty" should {
      "create InstantiatedRoute" in {
        Router.url(Root, HNil)
      }
      "not compile InstantiatedRoute with args" in {
        "Router.url(Root, 1 :: HNil)" shouldNot typeCheck
        """Router.url(Root, "asdf" :: HNil)""" shouldNot typeCheck
      }
    }
    "no Args" should {
      "create InstantiatedRoute" in {
        val r = Root / "asdf"
        Router.url(r, HNil)
      }
      "not compile InstantiatedRoute with args" in {
        """
        val r = Root / "asdf"
        Router.url(r, 1 :: HNil)
        """ shouldNot typeCheck
        """
        val r = Root / "asdf"
        Router.url(r, "asdf" :: HNil)
        """.stripMargin shouldNot typeCheck
      }
    }
    "one Arg" should {
      "create InstantiatedRoute" in {
        val route = Root / "asdf" / Arg[Int]
        Router.url(route, 1 :: HNil)
      }
      "not compile with InstantiatedRoute with invalid arg type" in {
        """
        val r = Root / "asdf" / Arg[Int]
        Router.url(r, "Route" :: HNil)
        """ shouldNot typeCheck
      }
      "not compile with InstantiatedRoute with invalid arg number" in {
        """
        val r = Root / "asdf" / Arg[Int]
        Router.url(r, "Route" :: 1 :: HNil)
        """ shouldNot typeCheck
      }
    }
    "multiple Args" should {
      "create InstantiatedRoute" in {
        val r = Root / Arg[String] / "asdf" / Arg[Int]
        Router.url(r, "Route" :: 1 :: HNil)
      }
      "not compile with wrong argument order" in {
        """
        val r = Root / Arg[String] / "asdf" / Arg[Int]
        Router.url(r, 1 :: "Route" :: HNil)
        """ shouldNot typeCheck
      }
      "not compile with wrong argument number" in {
        """
        val r = Root / Arg[String] / "asdf" / Arg[Int]
        Router.url(r, 1 :: 1 :: 1 :: HNil)
        """ shouldNot typeCheck
        """
        val r = Root / Arg[String] / "asdf" / Arg[Int]
        Router.url(r, 1 :: HNil)
        """ shouldNot typeCheck
        """
        val r = Root / Arg[String] / "asdf" / Arg[Int]
        Router.url(r, "Route" :: 1 :: 1 :: HNil)
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
        assert(Router.url(r, HNil) == "/asdf")
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
        assert(Router.url(r, FooBar("dasd") :: HNil) == "/dasd")
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