lazy val root = (project in file(".")).enablePlugins(SbtWeb)

// Both cleancss and sourceMap enabled: our inline clean-css plugin must feed less.js's
// source map into clean-css (via extra.sourceMap.getExternalSourceMap) and then write the
// merged map back (via extra.sourceMap.setExternalSourceMap). Without setExternalSourceMap,
// the written .css.map would still be less.js's pre-minification map, whose generated-side
// positions point at lines that don't exist in the single-line minified CSS.
LessKeys.cleancss := true
LessKeys.sourceMap := true

val checkMinifiedSourceMap = taskKey[Unit]("check the source map reflects minified positions")

checkMinifiedSourceMap := {
  val mapFile = (Assets / WebKeys.public).value / "css" / "main.css.map"
  val cssFile = (Assets / WebKeys.public).value / "css" / "main.css"

  if (!mapFile.exists) sys.error(s"Expected source map file to exist at ${mapFile.getAbsolutePath}")
  if (!cssFile.exists) sys.error(s"Expected CSS file to exist at ${cssFile.getAbsolutePath}")

  val mapContents = IO.read(mapFile)
  val cssContents = IO.read(cssFile)

  // The map should be valid JSON v3.
  if (!mapContents.contains("\"version\":3")) {
    sys.error(s"Source map isn't a valid v3 source map: $mapContents")
  }
  // The map should reference main.less as a source.
  if (!mapContents.contains("\"main.less\"")) {
    sys.error(s"Source map doesn't reference main.less: $mapContents")
  }
  // The CSS should be minified (collapsed to one line, no extra whitespace).
  if (cssContents.contains("\n  ")) {
    sys.error(s"CSS output doesn't appear to be minified: $cssContents")
  }
  // The "mappings" field should be non-empty.
  val mappingsPattern = """"mappings":"([^"]*)"""".r
  val mappingsMatch = mappingsPattern.findFirstMatchIn(mapContents)
  mappingsMatch match {
    case Some(m) if m.group(1).isEmpty =>
      sys.error(s"Source map has empty mappings field: $mapContents")
    case None =>
      sys.error(s"Source map missing mappings field: $mapContents")
    case _ => // OK
  }
}
