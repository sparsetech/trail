package pl.metastack.metarouter

trait Page {
  def render(attach: Boolean): Unit
  def rendered(): Unit
  def redirect(): Unit
}