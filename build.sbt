lazy val `sbt-less` = project in file(".")

enablePlugins(SbtWebBase)

description := "sbt-web less plugin"

developers += Developer(
  "playframework",
  "The Play Framework Team",
  "contact@playframework.com",
  url("https://github.com/playframework")
)

// sbt-web and sbt-js-engine are version-pinned per cross-build variant. The sbt 2.x variants
// were only published starting with 1.6.0-M1 / 1.4.0-M1. The sbt 1.x line stays on the latest
// 1.5.x stable so that older sbt-web pipeline plugins (such as sbt-uglify, sbt-rjs, sbt-jshint)
// that haven't been updated to the new SbtWeb.syncMappings signature continue to resolve.
def crossSbtPlugin(name: String, sbt1Version: String, sbt2Version: String) =
  libraryDependencies += Defaults.sbtPluginExtra(
    "com.github.sbt" % name % (if (scalaBinaryVersion.value == "3") sbt2Version else sbt1Version),
    (pluginCrossBuild / sbtBinaryVersion).value,
    scalaBinaryVersion.value
  )
crossSbtPlugin("sbt-web",       sbt1Version = "1.5.8", sbt2Version = "1.6.0-M4")
crossSbtPlugin("sbt-js-engine", sbt1Version = "1.3.9", sbt2Version = "1.4.0-M4")

pluginCrossBuild / sbtVersion := {
  scalaBinaryVersion.value match {
    case "3" =>
      "2.0.0-RC11"
    case _ =>
      sbtVersion.value
  }
}

crossScalaVersions += "3.8.3"

scalacOptions -= "-Xfatal-warnings"
scalacOptions += "-Werror"
scalacOptions ++= {
  scalaBinaryVersion.value match {
    case "3" =>
      Nil
    case _ =>
      Seq("-Xsource:3", "-release:8")
  }
}

libraryDependencies ++= Seq(
  "org.webjars.npm" % "node-require-fallback" % "1.0.0",
  "org.webjars.npm" % "less" % "4.6.3", // sync with src/main/resources/lessc.js
  "org.webjars.npm" % "clone" % "2.1.2",
  "org.webjars.npm" % "mkdirp" % "0.5.6", // sync with src/main/resources/lessc.js
  "org.webjars.npm" % "clean-css" % "5.3.3", // sync with src/main/resources/lessc.js
  "org.webjars.npm" % "es6-promise" % "4.2.8", // sync with src/main/resources/lessc.js
)

// Customise sbt-dynver's behaviour to make it work with tags which aren't v-prefixed
ThisBuild / dynverVTagPrefix := false

// Sanity-check: assert that version comes from a tag (e.g. not a too-shallow clone)
// https://github.com/dwijnand/sbt-dynver/#sanity-checking-the-version
Global / onLoad := (Global / onLoad).value.andThen { s =>
  dynverAssertTagVersion.value
  s
}
