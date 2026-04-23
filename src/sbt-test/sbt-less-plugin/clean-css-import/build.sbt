import spray.json._

lazy val root = (project in file(".")).enablePlugins(SbtWeb)

LessKeys.cleancss := true
// Preserve @import url() statements. Without this, clean-css 5.x's default (`inline: 'local'`)
// strips unresolved @imports.
LessKeys.cleancssOptions := Map("inline" -> JsBoolean(false))

val checkImportPreserved = taskKey[Unit]("check that @import url() statements survive clean-css when inline=false")

checkImportPreserved := {
  val contents = IO.read((Assets / WebKeys.public).value / "css" / "main.css")
  val expectedSubstring = """@import url(/assets/css/external-theme.css)"""

  if (!contents.contains(expectedSubstring)) {
    sys.error(s"Output did not preserve @import url() statement.\n  Expected to contain: $expectedSubstring\n  Actual output:       $contents")
  }
}
