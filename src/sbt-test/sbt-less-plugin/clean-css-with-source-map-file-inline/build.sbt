lazy val root = (project in file(".")).enablePlugins(SbtWeb)

// cleancss + sourceMap + sourceMapFileInline combined. Verifies:
//   - exactly one sourceMappingURL data URI is emitted (not duplicated by clean-css + our plugin)
//   - no separate .css.map file is written
//   - the embedded map's `sources` still references the original main.less (source side)
//   - the embedded map's `mappings` field is non-empty (sanity check that clean-css's
//     merged map made it through — if our plugin forgot to call setExternalSourceMap,
//     the map would still be less.js's pre-minification map, whose generated-side
//     positions reference line numbers that don't exist in the single-line minified CSS)
LessKeys.cleancss := true
LessKeys.sourceMap := true
LessKeys.sourceMapFileInline := true

val checkInlineSourceMapWithCleancss = taskKey[Unit]("verify cleancss + sourceMapFileInline produces a single valid inline map")

checkInlineSourceMapWithCleancss := {
  val cssFile = (Assets / WebKeys.public).value / "css" / "main.css"
  val mapFile = (Assets / WebKeys.public).value / "css" / "main.css.map"

  if (!cssFile.exists) sys.error(s"Expected CSS file at ${cssFile.getAbsolutePath}")
  if (mapFile.exists) sys.error(s"Did not expect a separate .map file when sourceMapFileInline=true: ${mapFile.getAbsolutePath}")

  val css = IO.read(cssFile)

  // Exactly one sourceMappingURL, inline as a data URI.
  val count = """sourceMappingURL=data:""".r.findAllIn(css).size
  if (count != 1) sys.error(s"Expected exactly one inline sourceMappingURL, found $count: $css")

  // CSS should be minified (no indentation).
  if (css.contains("\n  ")) sys.error(s"CSS doesn't appear minified: $css")

  // Decode the inline map and verify it reflects the minified output (short mappings, main.less source).
  val dataUriPattern = """sourceMappingURL=data:application/json;base64,([A-Za-z0-9+/=]+)""".r
  dataUriPattern.findFirstMatchIn(css) match {
    case Some(m) =>
      val decoded = new String(java.util.Base64.getDecoder.decode(m.group(1)), "UTF-8")
      if (!decoded.contains("\"main.less\"")) {
        sys.error(s"Inline source map doesn't reference main.less: $decoded")
      }
      // The "mappings" field should be non-empty.
      val mappingsPattern = """"mappings":"([^"]*)"""".r
      mappingsPattern.findFirstMatchIn(decoded) match {
        case Some(mm) if mm.group(1).isEmpty =>
          sys.error(s"Inline source map has empty mappings: $decoded")
        case None =>
          sys.error(s"Inline source map missing mappings field: $decoded")
        case _ => // OK
      }
    case None =>
      sys.error(s"Could not find inline data URI sourceMappingURL in CSS: $css")
  }
}
