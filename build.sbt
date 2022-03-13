val Leaf       = "0.1.0"
val Scala2_11  = "2.11.12"
val Scala2_12  = "2.12.15"
val Scala2_13  = "2.13.8"
val Scala3     = "3.1.1"
val ScalaTest  = "3.2.11"

val SharedSettings = Seq(
  name         := "trail",
  organization := "tech.sparse",

  scalaVersion       := Scala2_13,
  crossScalaVersions := Seq(Scala3, Scala2_13, Scala2_12, Scala2_11),
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
  .settings(SharedSettings)
  .settings(skip in publish := true)

lazy val trail = crossProject(JSPlatform, JVMPlatform, NativePlatform)
  .in(file("."))
  .settings(SharedSettings)
  .settings(
    autoAPIMappings := true,
    apiMappings += (scalaInstance.value.libraryJar -> url(s"http://www.scala-lang.org/api/${scalaVersion.value}/")),
    libraryDependencies ++= Seq("freespec", "wordspec", "funspec", "funsuite", "shouldmatchers").map(module =>
      "org.scalatest" %%% s"scalatest-$module" % ScalaTest % Test
    )
  )

lazy val trailNative = trail.native.settings(
  // ScalaTest is not yet published for Scala 3 Native
  libraryDependencies := {
    val deps = libraryDependencies.value

    if(isScala3(scalaVersion.value))
      deps.filterNot(_.organization == "org.scalatest")
    else deps
  },
  Test / test := {
    if(isScala3(scalaVersion.value)) {} else (Test / test).value
  }
)

lazy val manual = project.in(file("manual"))
  .dependsOn(trail.jvm)
  .settings(SharedSettings)
  .settings(
    name := "manual",
    publishArtifact := false,
    libraryDependencies += "tech.sparse" %% "leaf-notebook" % Leaf
  )

def isScala3(ver: String) = 
  CrossVersion.partialVersion(ver) match {
    case Some((3, _)) => true
    case _ => false
  }
