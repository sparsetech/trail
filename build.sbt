val MetaDocs   = "0.1.1"
val Shapeless  = "2.3.2"
val Scala2_11  = "2.11.8"
val Scala2_12  = "2.12.0"
val ScalaTest  = "3.0.1"
val ScalaJsDom = "0.9.1"
val Cats       = "0.8.1"
val JGit       = "4.5.0.201609210915-r"

val SharedSettings = Seq(
  name := "MetaRouter",
  organization := "pl.metastack",
  scalaVersion := Scala2_12,
  crossScalaVersions := Seq(Scala2_12, Scala2_11),
  scalacOptions := Seq(
    "-unchecked",
    "-deprecation",
    "-encoding", "utf8"
  ),
  pomExtra :=
    <url>https://github.com/MetaStack-pl/MetaRouter</url>
    <licenses>
      <license>
        <name>Apache-2.0</name>
        <url>https://www.apache.org/licenses/LICENSE-2.0.html</url>
      </license>
    </licenses>
    <scm>
      <url>git@github.com:MetaStack-pl/MetaRouter.git</url>
    </scm>
    <developers>
      <developer>
        <id>tindzk</id>
        <name>Tim Nieradzik</name>
        <url>http://github.com/tindzk/</url>
      </developer>
    </developers>
)

lazy val root = project.in(file("."))
  .aggregate(js, jvm)
  .settings(SharedSettings: _*)
  .settings(publishArtifact := false)

lazy val metaRouter = crossProject.in(file("."))
  .settings(SharedSettings: _*)
  .settings(
    autoAPIMappings := true,
    apiMappings += (scalaInstance.value.libraryJar -> url(s"http://www.scala-lang.org/api/${scalaVersion.value}/")),
    libraryDependencies ++= Seq(
      "com.chuusai"   %%% "shapeless" % Shapeless,
      "org.typelevel" %%% "cats"      % Cats,
      "org.scalatest" %%% "scalatest" % ScalaTest % "test"
    )
  )
  .jsSettings(
    libraryDependencies += "org.scala-js" %%% "scalajs-dom" % ScalaJsDom,

    /* Use io.js for faster compilation of test cases */
    scalaJSStage in Global := FastOptStage
  )

lazy val js  = metaRouter.js
lazy val jvm = metaRouter.jvm

lazy val manual = project.in(file("manual"))
  .dependsOn(jvm)
  .enablePlugins(BuildInfoPlugin)
  .settings(SharedSettings: _*)
  .settings(
    publishArtifact := false,
    libraryDependencies ++= Seq(
      "pl.metastack" %% "metadocs" % MetaDocs,
      "org.eclipse.jgit" % "org.eclipse.jgit" % JGit),
    buildInfoKeys := Seq[BuildInfoKey](name, version, scalaVersion, sbtVersion),
    buildInfoPackage := "pl.metastack.metarouter",
    name := "MetaRouter manual")
