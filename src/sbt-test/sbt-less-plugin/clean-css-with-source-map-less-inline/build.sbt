lazy val root = (project in file(".")).enablePlugins(SbtWeb)

// cleancss + sourceMapLessInline: less.js embeds the original LESS in its sourcesContent,
// then our post-processor hands that map to clean-css and reconstructs the merged output
// map. Verifies that sourcesContent survives the round-trip (clean-css's SourceMapGenerator
// naturally drops it because the output `sources` are renamed to `$stdin`; our plugin
// restores both fields from the original map).
LessKeys.cleancss := true
LessKeys.sourceMap := true
LessKeys.sourceMapLessInline := true

val checkLessSourcePreservedThroughCleancss = taskKey[Unit]("verify sourcesContent survives cleancss when sourceMapLessInline=true")

checkLessSourcePreservedThroughCleancss := {
  val mapFile = (Assets / WebKeys.public).value / "css" / "main.css.map"
  val map = IO.read(mapFile)

  if (!map.contains("\"sourcesContent\"")) {
    sys.error(s"cleancss + sourceMapLessInline=true dropped sourcesContent from the map: $map")
  }
  // The embedded source should be the actual LESS content, not null.
  if (!map.contains("color: blue")) {
    sys.error(s"sourcesContent was present but didn't carry the original LESS source: $map")
  }
}
