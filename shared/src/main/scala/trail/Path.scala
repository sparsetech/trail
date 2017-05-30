package trail

case class Path(path: String, args: Map[String, String] = Map.empty) {
  def url: String =
    if (args.isEmpty) path
    else path + "?" + args.map { case (k, v) =>
      k + "=" + URI.encode(v)
    }.mkString("&")
}

object PathParser {
  def parseParts(s: String): List[String] = {
    val x = s.stripPrefix("/")
    if (x.isEmpty) Nil
    else x.split('/').toList
  }

  def parseArgs(query: String): List[(String, String)] =
    query.split('&').flatMap { x =>
      val pair = x.split('=')
      if (pair.length != 2) List.empty
      else List((pair(0), URI.decode(pair(1))))
    }.toList

  /** Return URL without scheme, authority and fragment */
  def parsePathAndQuery(url: String): String = {
    val withoutScheme = url.indexOf("://") match {
      case -1 => url
      case i  => url.substring(i + 3)
    }

    val withoutAuthority = withoutScheme.indexOf('/') match {
      case -1 => withoutScheme
      case i  => withoutScheme.substring(i)
    }

    withoutAuthority.takeWhile(_ != '#')
  }

  def parse(url: String): Path = {
    val parsed = parsePathAndQuery(url).split('?')
    val (path, query) = (parsed.head, parsed.tail.headOption)
    val args = query.toList.flatMap(parseArgs).toMap

    Path(path, args)
  }
}