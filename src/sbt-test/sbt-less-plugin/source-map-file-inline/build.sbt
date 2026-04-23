lazy val root = (project in file(".")).enablePlugins(SbtWeb)

LessKeys.sourceMapFileInline := true

val checkSourceMapInlined = taskKey[Unit]("check that the source map is embedded in the CSS file, not written separately")

checkSourceMapInlined := {
  val cssFile = (Assets / WebKeys.public).value / "css" / "main.css"
  val mapFile = (Assets / WebKeys.public).value / "css" / "main.css.map"
  val contents = IO.read(cssFile)

  // When sourceMapFileInline is true, the CSS should contain a data-URI sourceMappingURL comment.
  val expectedMarker = "sourceMappingURL=data:application/json;base64,"
  if (!contents.contains(expectedMarker)) {
    sys.error(
      s"sourceMapFileInline=true did not inline the source map into the CSS output.\n" +
        s"  Expected output to contain: $expectedMarker\n" +
        s"  Actual output:\n$contents"
    )
  }

  // And there should NOT be a separate .map file when the source map is inlined.
  if (mapFile.exists) {
    sys.error(
      s"sourceMapFileInline=true should not produce a separate .map file, but ${mapFile.getAbsolutePath} exists."
    )
  }
}
