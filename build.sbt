val MetaDocs   = "0.1.1"
val Shapeless  = "2.3.1"
val Scala2_11  = "2.11.8"
val Scala2_12  = "2.12.0-M4"
val ScalaTest  = "3.0.0-RC2"
val ScalaJsDom = "0.9.1"

val SharedSettings = Seq(
  name := "MetaRouter",
  organization := "pl.metastack",
  scalaVersion := Scala2_11,
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
    apiMappings += (scalaInstance.value.libraryJar -> url(s"http://www.scala-lang.org/api/${scalaVersion.value}/"))
  )
  .jsSettings(
    libraryDependencies ++= Seq(
      "org.scala-js" %%% "scalajs-dom" % ScalaJsDom,
      "com.chuusai" %%% "shapeless" % Shapeless,
      "org.scalatest" %%% "scalatest" % ScalaTest % "test"
    ),

    /* Use io.js for faster compilation of test cases */
    scalaJSStage in Global := FastOptStage
  )
  .jvmSettings(
    libraryDependencies ++= Seq(
      "com.chuusai" %% "shapeless" % Shapeless,
      "org.scalatest" %% "scalatest" % ScalaTest % "test"
    )
  )

lazy val js = metaRouter.js
lazy val jvm = metaRouter.jvm

lazy val manual = project.in(file("manual"))
  .dependsOn(jvm)
  .enablePlugins(BuildInfoPlugin)
  .settings(SharedSettings: _*)
  .settings(
    publishArtifact := false,
    libraryDependencies ++= Seq(
      "pl.metastack" %% "metadocs" % MetaDocs,
      "org.eclipse.jgit" % "org.eclipse.jgit" % "4.1.1.201511131810-r"),
    buildInfoKeys := Seq[BuildInfoKey](name, version, scalaVersion, sbtVersion),
    buildInfoPackage := "pl.metastack.metarouter",
    name := "MetaRouter manual")
