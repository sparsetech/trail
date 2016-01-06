package pl.metastack.metarouter

import java.net.URLEncoder

object URI {
  /** @see http://stackoverflow.com/questions/607176/ */
  def encode(s: String): String =
    URLEncoder.encode(s, "UTF-8")
      .replaceAll("\\+", "%20")
      .replaceAll("\\%21", "!")
      .replaceAll("\\%27", "'")
      .replaceAll("\\%28", "(")
      .replaceAll("\\%29", ")")
      .replaceAll("\\%7E", "~")
}
