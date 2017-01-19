package pl.metastack.metarouter

import org.scalatest._

class RouterTests extends FlatSpec with Matchers {
  "Routing table" should "work" in {
    import Router.{route, url}

    case class Details(id: Int)
    case class UserInfo(user: String, details: Boolean)
    case class Register()

    implicit def details  = route[Details](Root / "details" / Arg[Int])
    implicit def userInfo = route[UserInfo](Root / "user" / Arg[String] / Arg[Boolean])
    implicit def register = route[Register](Root / "register")

    assert(url(Details(42)) == "/details/42")
    assert(url(UserInfo("test", true)) == "/user/test/true")
    assert(url(Register()) == "/register")
  }
}
