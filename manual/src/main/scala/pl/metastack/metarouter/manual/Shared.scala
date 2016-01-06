package pl.metastack.metarouter.manual

import java.io.File

import pl.metastack.metarouter.BuildInfo

trait Shared {
  val organisation = "MetaStack-pl"
  val repoName = s"$organisation.github.io"
  val projectName = "metarouter"
  val projectPath = new File("..", repoName)
  val manualPath = new File(projectPath, projectName)
  val manualPathStr = manualPath.getPath
  val manualVersionPath = new File(manualPath, "v" + BuildInfo.version)
  val manualVersionPathStr = manualVersionPath.getPath
  val isSnapshot = BuildInfo.version.endsWith("SNAPSHOT")
}
