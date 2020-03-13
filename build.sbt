import scala.sys.process._
val hash = "git rev-parse HEAD".!!.trim

name := "website"
organization := "au.id.jazzy.www"
version := hash.take(7)

lazy val root = (project in file("."))
  .enablePlugins(PlayScala, LauncherJarPlugin, DockerPlugin)

resolvers += Resolver.bintrayRepo("jroper", "maven")

libraryDependencies ++= Seq(
  filters,
  "au.id.jazzy.erqx" %% "erqx-engine" % "2.3.0"
)

scalaVersion := "2.13.1"

pipelineStages := Seq(gzip, digest)
excludeFilter in digest := "*.map" || "*.gz"

publishArtifact in (Compile, packageSrc) := false
publishArtifact in (Compile, packageDoc) := false
PlayKeys.includeDocumentationInBinary := false

sourceGenerators in Compile += Def.task {
  val dir = (sourceManaged in Compile).value
  val file = dir / "themes" / "Build.scala"
  if (!file.exists || !IO.read(file).contains(hash)) {
    IO.write(file,
      """ |package themes
          |
          |object Build {
          |  val hash = "%s"
          |}
        """.stripMargin.format(hash))
  }
  Seq(file)
}.taskValue

import com.typesafe.sbt.packager.docker.Cmd

dockerCommands := dockerCommands.value.map {
  case Cmd("FROM", unnamed) => Cmd("FROM", unnamed, "as", "main")
  case other => other
} ++ Seq(
  Cmd("FROM", "alpine/git", "as", "git"),
  Cmd("WORKDIR", "/opt/docker"),
  Cmd("RUN", "git", "clone", "https://github.com/jroper/jazzyidau.git", "blogs", "--no-checkout"),
  Cmd("FROM", "main"),
  Cmd("COPY", "--from=git", "--chown=1001", "/opt/docker/blogs", "/var/blogs/allthatjazz"),
  Cmd("COPY", "--from=git", "--chown=1001", "/opt/docker/blogs", "/var/blogs/ropedin"),
  Cmd("ENV", "ALLTHATJAZZ_REPO=/var/blogs/allthatjazz", "ROPEDIN_REPO=/var/blogs/ropedin"),
)

dockerUsername := Some("jamesroper")
