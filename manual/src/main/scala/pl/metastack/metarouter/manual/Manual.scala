package pl.metastack.metarouter.manual

import java.nio.file.{Files, Paths}

import org.eclipse.jgit.api.Git
import org.joda.time.DateTime

import pl.metastack.metadocs.document._
import pl.metastack.metadocs.input._
import pl.metastack.metadocs.input.metadocs._
import pl.metastack.metadocs.output.html.Components
import pl.metastack.metadocs.output.html.document.{Book, SinglePage}

import pl.metastack.metarouter.BuildInfo

object Manual extends App with Shared {
  if (!projectPath.exists())
    Git.cloneRepository()
      .setURI(s"git@github.com:$organisation/$repoName.git")
      .setDirectory(projectPath)
      .call()

  manualPath.mkdir()
  manualVersionPath.mkdirs()
  manualVersionPath.listFiles().foreach(_.delete())

  val meta = Meta(
    date = DateTime.now(),
    title = "MetaRouter User Manual v" + BuildInfo.version,
    author = "Tim Nieradzik",
    affiliation = "MetaStack Sp. z o.o.",
    `abstract` = "Routing library for Scala and Scala.js",
    language = "en-GB",
    url = "",
    editSourceURL = Some("https://github.com/MetaStack-pl/MetaRouter/edit/master/"))

  val instructionSet = DefaultInstructionSet
    .inherit(BookInstructionSet)
    .inherit(CodeInstructionSet)
    .inherit(DraftInstructionSet)
    .withAliases(
      "b" -> Bold,
      "i" -> Italic,
      "li" -> ListItem)

  val rawTrees = Seq(
    "introduction", "development", "support"
  ).map(chapter => s"manual/$chapter.md")
   .map(file =>
      Markdown.loadFileWithExtensions(file,
        instructionSet,
        constants = Map("version" -> BuildInfo.version),
        generateId = caption => Some(caption.collect {
          case c if c.isLetterOrDigit => c
          case c if c.isSpaceChar => '-'
        }.toLowerCase)
      ).get)

  val docTree = Document.mergeTrees(rawTrees)

  // Explicitly print out all chapters/sections which is useful when
  // restructuring the document
  println("Document tree:")
  println(Extractors.references(docTree))
  println()

  Document.printTodos(docTree)

  val pipeline =
    Document.pipeline
      .andThen(CodeProcessor.embedListings("manual") _)
      .andThen(CodeProcessor.embedOutput _)
  val docTreeWithCode = pipeline(docTree)

  val skeleton = Components.pageSkeleton(
    cssPaths = Seq(
      "css/kult.css",
      "css/default.min.css"
    ),
    jsPaths = Seq(
      "//ajax.googleapis.com/ajax/libs/jquery/2.1.1/jquery.min.js",
      "js/main.js",
      "js/highlight.pack.js"
    ),
    script = Some("hljs.initHighlightingOnLoad();"),
    favicon = Some("favicon.ico")
  )(_, _, _)

  // TODO Copy scaladocs as well

  SinglePage.write(docTreeWithCode,
    skeleton,
    s"$manualPathStr/v${BuildInfo.version}.html",
    meta = Some(meta),
    toc = true,
    tocDepth = 2)  // Don't include subsections

  Book.write(docTreeWithCode,
    skeleton,
    s"$manualPathStr/v${BuildInfo.version}",
    meta = Some(meta),
    tocDepth = 2)

  val links =
    Set("css", "js", "favicon.ico").flatMap { folder =>
      Seq(
        s"$manualPathStr/$folder" -> s"../$folder",
        s"$manualVersionPathStr/$folder" -> s"../../$folder")
    }.toMap ++
      (if (isSnapshot) Map.empty
       else Map(
         s"$manualPathStr/latest" -> s"v${BuildInfo.version}",
         s"$manualPathStr/latest.html" -> s"v${BuildInfo.version}.html"))

  links.map { case (from, to) =>
    Paths.get(from) -> Paths.get(to)
  }.foreach { case (from, to) =>
    if (Files.exists(from)) Files.delete(from)
    Files.createSymbolicLink(from, to)
  }
}
