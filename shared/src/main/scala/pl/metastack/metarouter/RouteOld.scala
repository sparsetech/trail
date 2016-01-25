package pl.metastack.metarouter

case class RouteOld(path: String, args: Map[String, String] = Map.empty) {
  def url: String = {
    val base =
      if (path.startsWith("http:") ||
        path.startsWith("https:") ||
        path.startsWith("mailto:")) path
      else s"/$path"

    if (args.isEmpty) base
    else base + "?" + args.map { case (k, v) =>
      k + "=" + URI.encode(v)
    }.mkString("&")
  }
}
