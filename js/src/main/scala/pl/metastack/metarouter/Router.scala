package pl.metastack.metarouter

import scala.scalajs.js.URIUtils

import org.scalajs.dom

trait Router {
  var currentPage = Option.empty[Page]

  def dispatch(route: RouteOld, attach: Boolean)

  def replaceUrl(route: RouteOld): Unit = {
    dom.document.body.scrollTop = 0
    dom.window.history.pushState("", "", route.url)
  }

  def render(page: Page, attach: Boolean = false): Unit = {
    currentPage.foreach(_.redirect())
    currentPage = Some(page)

    page.render(attach)
    page.rendered()
  }

  def parseRoute(href: String): RouteOld = {
    val uri = href.takeWhile(_ != '#').split('/').drop(3).toSeq.mkString("/").split('?')
    val (path, args) = (uri.head, uri.tail.headOption)
    val parsedArgs = args.map { a =>
      val split = a.split('&')
      split.map { s =>
        val parts = s.split('=').map(URIUtils.decodeURIComponent)
        if (parts.length == 1) (parts(0), "")
        else (parts(0), parts(1))
      }.toSeq
    }.getOrElse(Seq.empty).toMap

    RouteOld(path, parsedArgs)
  }

  def register() {
    dom.window.onload = { (e: dom.Event) =>
      dispatch(parseRoute(dom.window.location.href), attach = true)
    }

    dom.window.onpopstate = { (e: dom.PopStateEvent) =>
      dispatch(parseRoute(dom.document.location.href), attach = false)
    }
  }
}