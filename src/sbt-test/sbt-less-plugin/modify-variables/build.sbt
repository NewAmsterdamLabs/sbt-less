lazy val root = (project in file(".")).enablePlugins(SbtWeb)

// modifyVariables should override a variable that's already defined in the source file.
LessKeys.modifyVariables := Seq("color" -> "blue")

val checkVariableModified = taskKey[Unit]("check that modifyVariables overrode the file-defined @color")

checkVariableModified := {
  val contents = IO.read((Assets / WebKeys.public).value / "css" / "main.css")

  if (!contents.contains("color: blue")) {
    sys.error(
      s"modifyVariables did not override @color.\n" +
        s"  Expected output to contain: color: blue\n" +
        s"  Actual output:\n$contents"
    )
  }
}
