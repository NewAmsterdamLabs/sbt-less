lazy val root = (project in file(".")).enablePlugins(SbtWeb)

LessKeys.sourceMapRootpath := "/my/root/"

val checkSourceMapRootpath = taskKey[Unit]("check that sourceMapRootpath is prepended to sources in the source map")

checkSourceMapRootpath := {
  val mapFile = (Assets / WebKeys.public).value / "css" / "main.css.map"
  val contents = IO.read(mapFile)

  // less.js prepends sourceMapRootpath to each source path in the map.
  // Expected: "sources":["/my/root/main.less"] (relativeImports may also rewrite, so just verify rootpath is present).
  if (!contents.contains("/my/root/")) {
    sys.error(
      s"sourceMapRootpath='/my/root/' was not applied to the source map.\n" +
        s"  Expected map to contain: /my/root/\n" +
        s"  Actual map:\n$contents"
    )
  }
}
