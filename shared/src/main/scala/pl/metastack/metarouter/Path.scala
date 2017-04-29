package pl.metastack.metarouter

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

  def parse(url: String): Path = {
    val withoutProtocol = url.indexOf("://") match {
      case -1 => url
      case i  => url.substring(i + 3)
    }

    val withoutDomain = withoutProtocol.indexOf('/') match {
      case -1 => withoutProtocol
      case i  => withoutProtocol.substring(i)
    }

    val parsed = withoutDomain.takeWhile(_ != '#').split('?')
    val (path, query) = (parsed.head, parsed.tail.headOption)
    val args = query.toList.flatMap(parseArgs).toMap

    Path(path, args)
  }
}