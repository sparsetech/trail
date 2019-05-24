package trail

case class Path(path: String,
                args: List[(String, String)] = List(),
                fragment: Option[String] = None) {
  def url: String = (
    if (args.isEmpty) path
    else path + "?" + args.map { case (k, v) =>
      k + "=" + URI.encode(v)
    }.mkString("&")
  ) + fragment.fold("")("#" + _)
}

object PathParser {
  def parseParts(s: String): List[String] = {
    val x = s.stripPrefix("/")
    if (x.isEmpty) Nil
    else x.split('/').toList
  }

  def parseArgs(query: String): List[(String, String)] =
    query.split('&').flatMap { x =>
      val equalSign = x.indexOf('=')
      if (equalSign == -1) List()
      else List((x.take(equalSign), URI.decode(x.drop(equalSign + 1))))
    }.toList

  /** Return URL without scheme and authority */
  private[trail] def dropSchemeAndAuthority(url: String): String = {
    val withoutScheme = url.indexOf("://") match {
      case -1 => url
      case i  => url.substring(i + 3)
    }

    val withoutAuthority = withoutScheme.indexOf('/') match {
      case -1 => withoutScheme
      case i  => withoutScheme.substring(i)
    }

    withoutAuthority
  }

  private def parseFragment(input: String): (String, Option[String]) =
    input.indexOf('#') match {
      case -1 => (input, None)
      case n =>
        val (q, f) = input.splitAt(n)
        (q, Some(f.tail))
    }

  def parse(url: String): Path = {
    val pathRaw = dropSchemeAndAuthority(url)

    val (path, args, fragment): (String, List[(String, String)], Option[String]) =
      pathRaw.indexOf('?') match {
        case -1 =>
          val (path, fragment) = parseFragment(pathRaw)
          (path, List(), fragment)
        case n =>
          val (path, queryRaw) = pathRaw.splitAt(n)
          val (query, fragment) = parseFragment(queryRaw)
          (path, parseArgs(query.tail), fragment)
      }

    Path(path, args, fragment)
  }
}