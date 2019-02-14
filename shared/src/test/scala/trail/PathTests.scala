package trail

import org.scalatest._

class PathTests extends FunSuite {
  test("Parse absolute URL") {
    assert(PathParser.parse("http://example.com/test") == Path("/test"))
  }

  test("Parse path and query components") {
    assert(PathParser.dropSchemeAndAuthority("http://example.com/test") == "/test")
    assert(PathParser.dropSchemeAndAuthority("http://example.com/test?a=b") == "/test?a=b")
    assert(PathParser.dropSchemeAndAuthority("http://example.com/test?a=b#frag") == "/test?a=b#frag")
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

  // See https://en.wikipedia.org/wiki/Fragment_identifier
  val FragmentUrls =
    Set(
      "/test#"  -> Path("/test", fragment = Some("")),
      "/test#a" -> Path("/test", fragment = Some("a")),
      "/bar.webm?a=b#t=40,80&xywh=160,120,320,240" ->
        Path("/bar.webm", Map("a" -> "b"), Some("t=40,80&xywh=160,120,320,240")))

  test("Parse fragment") {
    FragmentUrls.foreach { case (url, parsed) =>
      assert(PathParser.parse(url) == parsed)
    }
  }

  test("Generate URL") {
    val url = "/test?a=b&c=d"
    assert(PathParser.parse(url).url == url)
  }

  test("Generate URL with special characters") {
    val url = "/test?t=%C3%A4%C3%B3%C3%A7"
    assert(PathParser.parse(url).url == url)
  }

  test("Generate URL with fragment") {
    FragmentUrls.foreach { case (url, parsed) =>
      assert(parsed.url == url)
    }
  }
}