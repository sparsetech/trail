package pl.metastack.metarouter.manual

import java.io.File

import org.eclipse.jgit.api.Git
import org.eclipse.jgit.storage.file.FileRepositoryBuilder

import pl.metastack.metarouter.BuildInfo

object Deploy extends App with Shared {
  val repo = FileRepositoryBuilder.create(new File(projectPath, ".git"))
  val git = new Git(repo)
  git.add().addFilepattern(projectName).call()
  if (!git.status().call().isClean) {
    git.commit()
      .setAll(true)
      .setMessage(s"Update $projectName v${BuildInfo.version}").call()
    git.push().call()
  }
}
