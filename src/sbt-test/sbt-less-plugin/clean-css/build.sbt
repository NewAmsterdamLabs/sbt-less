lazy val root = (project in file(".")).enablePlugins(SbtWeb)

LessKeys.cleancss := true

val checkCleanCssUsed = taskKey[Unit]("check that clean-css has been used")

checkCleanCssUsed := {
  val contents = IO.read((Assets / WebKeys.public).value / "css" / "main.css")
  // The CSS should be minified (clean-css collapsed the rule); the sourceMappingURL
  // comment is appended by less.js 4.x when sourceMap is enabled (the default).
  val expectedContents = """h1{color:#00f}/*# sourceMappingURL=main.css.map */"""

  if (contents != expectedContents) {
    sys.error(s"Unexpected contents: $contents, \nexpected: $expectedContents")
  }
}
