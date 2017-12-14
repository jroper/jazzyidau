name := "website"
organization := "au.id.jazzy.www"
version := "1.0.0-SNAPSHOT"

lazy val root = (project in file("."))
  .enablePlugins(PlayScala, LauncherJarPlugin)

resolvers += Resolver.bintrayRepo("jroper", "maven")

libraryDependencies ++= Seq(
  filters,
  "au.id.jazzy.erqx" %% "erqx-engine" % "2.1.5"
)

scalaVersion := "2.12.4"

pipelineStages := Seq(gzip, digest)
excludeFilter in digest := "*.map" || "*.gz"

sourceGenerators in Compile += Def.task {
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

