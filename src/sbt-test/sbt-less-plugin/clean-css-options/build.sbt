import spray.json._

lazy val root = (project in file(".")).enablePlugins(SbtWeb)

LessKeys.cleancss := true

// Request clean-css to keep line breaks between rules. Without this option, clean-css collapses
// everything onto a single line (e.g., "h1{color:#00f}h2{color:red}"). With it, each rule ends
// with a newline, so the output contains at least one "\n" between the rule bodies.
LessKeys.cleancssOptions := Map("format" -> JsString("keep-breaks"))

val checkCleancssOptionsApplied = taskKey[Unit]("check that cleancssOptions were actually applied")

checkCleancssOptionsApplied := {
  val contents = IO.read((Assets / WebKeys.public).value / "css" / "main.css")

  // With --keep-line-breaks, there should be a newline somewhere in the output between the two rules.
  // Without it, the output is a single line.
  if (!contents.contains("\n")) {
    sys.error(s"""cleancssOptions 'format' -> 'keep-breaks' was not applied. Expected output to contain newlines, but it was collapsed to a single line: $contents""")
  }
}
