lazy val root = (project in file(".")).enablePlugins(SbtWeb)

// urlArgs appends a query string to every url() token in the output.
LessKeys.urlArgs := "v=123"

val checkUrlArgsApplied = taskKey[Unit]("check that urlArgs is appended to url() tokens")

checkUrlArgsApplied := {
  val contents = IO.read((Assets / WebKeys.public).value / "css" / "main.css")

  if (!contents.contains("image.png?v=123")) {
    sys.error(
      s"urlArgs='v=123' was not appended to url() tokens.\n" +
        s"  Expected output to contain: image.png?v=123\n" +
        s"  Actual output:\n$contents"
    )
  }
}
