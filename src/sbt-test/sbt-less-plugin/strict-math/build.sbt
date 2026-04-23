lazy val root = (project in file(".")).enablePlugins(SbtWeb)

// With strictMath=true, less.js should NOT evaluate `10px + 5px`; it should leave the expression as-is.
// With the default (strictMath=false), the expression would be evaluated to `15px`.
LessKeys.strictMath := true

val checkStrictMath = taskKey[Unit]("check that strictMath=true prevents evaluation of math expressions")

checkStrictMath := {
  val contents = IO.read((Assets / WebKeys.public).value / "css" / "main.css")

  if (contents.contains("15px")) {
    sys.error(
      s"strictMath=true should have prevented the evaluation of '10px + 5px', but the output contains '15px'.\n" +
        s"  Actual output:\n$contents"
    )
  }
  if (!contents.contains("10px + 5px") && !contents.contains("10px+5px")) {
    sys.error(
      s"strictMath=true should have preserved '10px + 5px' as-is, but it's missing from the output.\n" +
        s"  Actual output:\n$contents"
    )
  }
}
