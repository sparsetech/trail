package trail

import scala.scalajs.js.URIUtils

object URI {
  def encode(s: String): String = URIUtils.encodeURIComponent(s)
  def decode(s: String): String = URIUtils.decodeURIComponent(s)
}
