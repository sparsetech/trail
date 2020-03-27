// shadow sbt-scalajs' crossProject and CrossType from Scala.js 0.6.x
import sbtcrossproject.CrossPlugin.autoImport.{crossProject, CrossType}

val Leaf       = "0.1.0"
val Scala2_11  = "2.11.12"
val Scala2_12  = "2.12.11"
val Scala2_13  = "2.13.1"
val ScalaTest  = "3.1.1"
val ScalaTestNative = "3.2.0-SNAP10"

val SharedSettings = Seq(
  name         := "trail",
  organization := "tech.sparse",

  scalaVersion       := Scala2_13,
  crossScalaVersions := Seq(Scala2_13, Scala2_12, Scala2_11),
  scalacOptions      := Seq(
    "-unchecked",
    "-deprecation",
    "-encoding", "utf8"
  ),

  pomExtra :=
    <url>https://github.com/sparsetech/trail</url>
    <licenses>
      <license>
        <name>Apache-2.0</name>
        <url>https://www.apache.org/licenses/LICENSE-2.0.html</url>
      </license>
    </licenses>
    <scm>
      <url>git@github.com:sparsetech/trail.git</url>
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
  .aggregate(trail.js, trail.jvm, trail.native)
  .settings(SharedSettings: _*)
  .settings(skip in publish := true)

lazy val trail = crossProject(JSPlatform, JVMPlatform, NativePlatform)
  .in(file("."))
  .settings(SharedSettings: _*)
  .settings(
    autoAPIMappings := true,
    apiMappings += (scalaInstance.value.libraryJar -> url(s"http://www.scala-lang.org/api/${scalaVersion.value}/")),
  )
  .jsSettings(
    libraryDependencies += "org.scalatest" %%% "scalatest" % ScalaTest % "test"
  )
  .jvmSettings(
    libraryDependencies += "org.scalatest" %%% "scalatest" % ScalaTest % "test"
  ).nativeSettings(
    scalaVersion       := Scala2_11,
    crossScalaVersions := Seq(Scala2_11),
    // See https://github.com/scalalandio/chimney/issues/78#issuecomment-419705142
    nativeLinkStubs    := true,
    libraryDependencies +=
      "org.scalatest" %%% "scalatest" % ScalaTestNative % "test"
  )

lazy val manual = project.in(file("manual"))
  .dependsOn(trail.jvm)
  .settings(SharedSettings: _*)
  .settings(
    name := "manual",
    publishArtifact := false,
    libraryDependencies += "tech.sparse" %% "leaf-notebook" % Leaf
  )
