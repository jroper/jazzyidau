
name := "website"

organization := "au.id.jazzy.wwww"

version := "1.0.0-SNAPSHOT"

lazy val root = (project in file(".")).enablePlugins(PlayScala)

resolvers += "jroper.github.io releases" at "https://jroper.github.io/releases"

libraryDependencies += "au.id.jazzy.erqx" %% "erqx-engine" % "1.0.0-RC1"

scalaVersion := "2.11.2"

pipelineStages := Seq(gzip, digest)

excludeFilter in digest := "*.map" || "*.gz"

sourceGenerators in Compile <+= sourceManaged in Compile map { dir =>
  val hash = ("git rev-parse HEAD" !!).trim
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
}

