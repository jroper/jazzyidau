name := "website"
organization := "au.id.jazzy.www"
version := "1.0.0-SNAPSHOT"

lazy val root = (project in file("."))
  .enablePlugins(PlayScala, LauncherJarPlugin)

resolvers += Resolver.bintrayRepo("jroper", "maven")

libraryDependencies ++= Seq(
  filters,
  "au.id.jazzy.erqx" %% "erqx-engine" % "2.2.0-RC1"
)

scalaVersion := "2.12.7"

pipelineStages := Seq(gzip, digest)
excludeFilter in digest := "*.map" || "*.gz"

publishArtifact in (Compile, packageSrc) := false
publishArtifact in (Compile, packageDoc) := false
PlayKeys.includeDocumentationInBinary := false

sourceGenerators in Compile += Def.task {
  import scala.sys.process._
  val dir = (sourceManaged in Compile).value
  val hash = "git rev-parse HEAD".!!.trim
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

