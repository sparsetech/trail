package pl.metastack.metarouter

import org.scalatest._

class CanFillRouteTests extends WordSpec with Matchers  {
  "A Route" when {
    "empty" should {
      "create InstantiatedRoute" in {
        Root.fill()
      }
      "not compile InstantiatedRoute with args" in {
        "Root.fill(1)" shouldNot typeCheck
        """Root.fill("asdf")""" shouldNot typeCheck
      }
    }
    "no Args" should {
      "create InstantiatedRoute" in {
        val r = Root / "asdf"
        r.fill()
      }
      "not compile InstantiatedRoute with args" in {
        """
        val r = Root / "asdf"
        r.fill(1)
        """ shouldNot typeCheck
        """
        val r = Root / "asdf"
        r.fill("asdf")
        """.stripMargin shouldNot typeCheck
      }
    }
    "one Arg" should {
      "create InstantiatedRoute" in {
        val route = Root / "asdf" / Arg[Int]("asf")
        route.fill(1)
      }
      "not compile with InstantiatedRoute with invalid arg type" in {
        """
        val r = Root / "asdf" / Arg[Int]("asf")
        r.fill("Route")
        """ shouldNot typeCheck
      }
      "not compile with InstantiatedRoute with invalid arg number" in {
        """
        val r = Root / "asdf" / Arg[Int]("asf")
        r.fill("Route", 1)
        """ shouldNot typeCheck
      }
    }
    "multiple Args" should {
      "create InstantiatedRoute" in {
        val r = Root / Arg[String]("db") / "asdf" / Arg[Int]("asf")
        r.fillN("Route", 1)
      }
      "not compile with wrong argument order" in {
        """
        val r = Root / Arg[String]("db") / "asdf" / Arg[Int]("asf")
        r.fill(1, "Route")
        """ shouldNot typeCheck
      }
      "not compile with wrong argument number" in {
        """
        val r = Root / Arg[String]("db") / "asdf" / Arg[Int]("asf")
        r.fill(1, 1, 1)
        """ shouldNot typeCheck
        """
        val r = Root / Arg[String]("db") / "asdf" / Arg[Int]("asf")
        r.fill(1)
        """ shouldNot typeCheck
        """
        val r = Root / Arg[String]("db") / "asdf" / Arg[Int]("asf")
        r.fill("Route", 1, 1)
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
        val i = r.fill()
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
        val r = Root / Arg[FooBar]("asdf")
        val i = r.fill(FooBar("dasd"))
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