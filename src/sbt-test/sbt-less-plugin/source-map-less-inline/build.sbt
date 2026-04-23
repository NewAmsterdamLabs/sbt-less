lazy val root = (project in file(".")).enablePlugins(SbtWeb)

LessKeys.sourceMapLessInline := true

val checkLessSourceEmbedded = taskKey[Unit]("check that the original LESS source is embedded in the source map")

checkLessSourceEmbedded := {
  val mapFile = (Assets / WebKeys.public).value / "css" / "main.css.map"
  val contents = IO.read(mapFile)

  // When sourceMapLessInline=true, the source map should include a sourcesContent array
  // containing the original LESS source code for each source file.
  if (!contents.contains("\"sourcesContent\"")) {
    sys.error(s"""sourceMapLessInline=true did not embed the LESS source in the map file. Expected map to contain "sourcesContent". Actual map: $contents""")
  }

  // Sanity-check: the embedded source should contain the actual LESS content
  if (!contents.contains("color: blue") && !contents.contains("color:blue")) {
    sys.error(s"""sourceMapLessInline=true produced a map with sourcesContent but the LESS source is missing. Actual map: $contents""")
  }
}
