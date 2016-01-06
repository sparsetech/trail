package pl.metastack.metarouter

import scala.scalajs.js.URIUtils

object URI {
  def encode(s: String): String = URIUtils.encodeURIComponent(s)
}
