package pl.metastack.metarouter

case class Path(path: String, args: Map[String, String] = Map.empty) {
  def url: String =
    if (args.isEmpty) path
    else path + "?" + args.map { case (k, v) =>
      k + "=" + URI.encode(v)
    }.mkString("&")
}

object PathParser {
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
    val (path, args) = (parsed.head, parsed.tail.headOption)

    val parsedArgs = args.map { a =>
      val split = a.split('&')

      split.map { s =>
        val parts = s.split('=').map(URI.decode)

        if (parts.length == 1) (parts(0), "")
        else (parts(0), parts(1))
      }.toSeq
    }.getOrElse(Seq.empty)
      .toMap

    new Path(path, parsedArgs)
  }
}