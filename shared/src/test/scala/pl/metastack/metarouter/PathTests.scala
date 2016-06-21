package pl.metastack.metarouter

import org.scalatest._

class PathTests extends FunSuite {
  test("Parse absolute URL") {
    assert(PathParser.parse("http://example.com/test") == Path("/test"))
  }

  test("Parse relative URL (root)") {
    assert(PathParser.parse("/") == Path("/"))
  }

  test("Parse relative URL") {
    assert(PathParser.parse("/test") == Path("/test"))
  }

  test("Parse absolute URL with parameters") {
    assert(PathParser.parse("http://example.com/test?a=b&c=d") ==
      Path("/test", Map("a" -> "b", "c" -> "d")))
  }

  test("Parse relative URL with special characters in parameter") {
    assert(PathParser.parse("/test?a=%C3%A4%C3%B3%C3%A7") ==
      Path("/test", Map("a" -> "äóç")))
  }

  test("Ignore hash tag") {
    assert(PathParser.parse("http://example.com/test#a") == Path("/test"))
  }

  test("Generate URL") {
    val url = "/test?a=b&c=d"
    assert(PathParser.parse(url).url == url)
  }

  test("Generate URL with special characters") {
    val url = "/test?t=%C3%A4%C3%B3%C3%A7"
    assert(PathParser.parse(url).url == url)
  }
}