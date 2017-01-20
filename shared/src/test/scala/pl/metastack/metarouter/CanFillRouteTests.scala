package pl.metastack.metarouter

import org.scalatest._
import shapeless.HNil

class CanFillRouteTests extends WordSpec with Matchers  {
  "A Route" when {
    "empty" should {
      "create InstantiatedRoute" in {
        Router.fill(Root)
      }
      "not compile InstantiatedRoute with args" in {
        "Router.fill(Root, 1 :: HNil)" shouldNot typeCheck
        """Router.fill(Root, "asdf" :: HNil)""" shouldNot typeCheck
      }
    }
    "no Args" should {
      "create InstantiatedRoute" in {
        val r = Root / "asdf"
        Router.fill(r, HNil)
      }
      "not compile InstantiatedRoute with args" in {
        """
        val r = Root / "asdf"
        Router.fill(r, 1 :: HNil)
        """ shouldNot typeCheck
        """
        val r = Root / "asdf"
        Router.fill(r, "asdf" :: HNil)
        """.stripMargin shouldNot typeCheck
      }
    }
    "one Arg" should {
      "create InstantiatedRoute" in {
        val route = Root / "asdf" / Arg[Int]
        Router.fill(route, 1 :: HNil)
      }
      "not compile with InstantiatedRoute with invalid arg type" in {
        """
        val r = Root / "asdf" / Arg[Int]
        Router.fill(r, "Route" :: HNil)
        """ shouldNot typeCheck
      }
      "not compile with InstantiatedRoute with invalid arg number" in {
        """
        val r = Root / "asdf" / Arg[Int]
        Router.fill(r, "Route" :: 1 :: HNil)
        """ shouldNot typeCheck
      }
    }
    "multiple Args" should {
      "create InstantiatedRoute" in {
        val r = Root / Arg[String] / "asdf" / Arg[Int]
        Router.fill(r, "Route" :: 1 :: HNil)
      }
      "not compile with wrong argument order" in {
        """
        val r = Root / Arg[String] / "asdf" / Arg[Int]
        Router.fill(r, 1 :: "Route" :: HNil)
        """ shouldNot typeCheck
      }
      "not compile with wrong argument number" in {
        """
        val r = Root / Arg[String] / "asdf" / Arg[Int]
        Router.fill(r, 1 :: 1 :: 1 :: HNil)
        """ shouldNot typeCheck
        """
        val r = Root / Arg[String] / "asdf" / Arg[Int]
        Router.fill(r, 1 :: HNil)
        """ shouldNot typeCheck
        """
        val r = Root / Arg[String] / "asdf" / Arg[Int]
        Router.fill(r, "Route" :: 1 :: 1 :: HNil)
        """ shouldNot typeCheck
      }
    }
    "custom path element" should {
      case class FooBar(foo: String)
      implicit object FooStaticElement extends StaticElement[FooBar] {
        def urlEncode(value: FooBar): String = value.foo
      }
      "create an InstantiatedRoute" in {
        val r = Root / FooBar("asdf")
        val i = Router.fill(r, HNil)
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
      "create an InstantiatedRoute" in {
        val r = Root / Arg[FooBar]
        val i = Router.fill(r, FooBar("dasd") :: HNil)
        import shapeless._
        assert(i.data === FooBar("dasd") :: HNil)
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