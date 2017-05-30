package trail

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
      case class Foo(bar: String)
      implicit object FooElement extends StaticElement[Foo](_.bar)
      "create URL" in {
        val r = Root / Foo("asdf")
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
      case class Foo(foo: String)
      implicit object FooCodec extends Codec[Foo] {
        override def encode(s: Foo): String = s.foo
        override def decode(s: String): Option[Foo] = Option(s).map(Foo)
      }
      "create URL" in {
        val r = Root / Arg[Foo]
        assert(r(Foo("dasd") :: HNil) == "/dasd")
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