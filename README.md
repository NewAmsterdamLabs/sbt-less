sbt-less
========

[![Build Status](https://github.com/sbt/sbt-less/actions/workflows/build-test.yml/badge.svg)](https://github.com/sbt/sbt-less/actions/workflows/build-test.yml)

Allows less to be used from within sbt. Builds on com.github.sbt:js-engine in order to execute the less compiler along with
the scripts to verify. js-engine enables high performance linting given parallelism and native JS engine execution.

To use this plugin use the addSbtPlugin command within your project's plugins.sbt (or as a global setting) i.e.:

```scala
addSbtPlugin("com.github.sbt" % "sbt-less" % "2.0.0")
```

Your project's build file also needs to enable sbt-web plugins. For example with build.sbt:

    lazy val root = (project in file(".")).enablePlugins(SbtWeb)

The compiler allows most of the same options to be specified as the [lessc CLI itself](http://lesscss.org/usage/).
Here are the options:

Option              | Description
--------------------|------------
cleancss            | Compress output using clean-css.
cleancssOptions     | Pass an option to clean css, using CLI arguments from https://github.com/GoalSmashers/clean-css .
color               | Whether LESS output should be colorised
compress            | Compress output by removing some whitespaces.
globalVariables     | Variables that will be placed at the top of the less file.
ieCompat            | Do IE compatibility checks.
insecure            | Allow imports from insecure https hosts.
maxLineLen          | Maximum line length.
modifyVariables     | Modifies a variable already declared in the file.
optimization        | Set the parser's optimization level.
relativeImports     | Re-write import paths relative to the base less file. Default is true.
relativeUrls        | Re-write relative urls to the base less file.
rootpath            | Set rootpath for url rewriting in relative imports and urls.
silent              | Suppress output of error messages.
sourceMap           | Outputs a v3 sourcemap.
sourceMapFileInline | Whether the source map should be embedded in the output file
sourceMapLessInline | Whether to embed the less code in the source map
sourceMapRootpath   | Adds this path onto the sourcemap filename and less file paths.
strictImports       | Whether imports should be strict.
strictMath          | Requires brackets. This option may default to true and be removed in future.
strictUnits         | Whether all unit should be strict, or if mixed units are allowed.
urlArgs             | Adds params into url tokens (e.g. 42, cb=42 or 'a=1&b=2').
verbose             | Be verbose.
    
The following sbt code illustrates how compression can be enabled:

```scala
Assets / LessKeys.compress := true
```

By default only `main.less` is looked for given that the LESS compiler must be explicitly fed the files
that are required for compilation. Beyond just `main.less`, you can use an expression in your `build.sbt` like the
following:

```scala
Assets / LessKeys.less / includeFilter := "foo.less" | "bar.less"
```

...where both `foo.less` and `bar.less` will be considered for the LESS compiler.

Alternatively you may want a more general expression to exclude LESS files that are not considered targets
for the compiler. Quite commonly, LESS files are divided up into those entry point files and other files, with the
latter set intended for importing into the entry point files. These other files tend not to be suitable for the
compiler in isolation as they can depend on the global declarations made by other non-imported LESS files. For example,
you may have a convention where any LESS file starting with an `_` should not be considered for direct compilation. To
include all `.less` files but exclude any beginning with an `_` you can use the following declaration:

```scala
Assets / LessKeys.less / includeFilter := "*.less"

Assets / LessKeys.less / excludeFilter := "_*.less"
```

## Publishing (Zola fork)

This fork publishes to Zola's Nexus instead of Sonatype Central. The
publish destination, `organization`, and `publishMavenStyle` settings
live in `publishZola.sbt` at the project root.

For the symlink to resolve, the `tools` repo must be cloned as a
sibling of this one (i.e. both at the same parent directory).

### Versioning

The version is derived by `sbt-dynver` from git, with
`dynverVTagPrefix := false` so tags do not need a `v` prefix:

- **Clean tree, HEAD on a tag** → `version` is the tag verbatim, e.g.
  `2.1.0-ZOLA`. `isSnapshot` is `false`, so artifacts go to the Nexus
  releases repo.
- **Past the tag or dirty tree** → version becomes
  `<tag>+<n>-<sha>+<ts>-SNAPSHOT`. `isSnapshot` is `true`, so artifacts
  go to the Nexus snapshots repo.

To cut a Zola release, tag the commit (e.g. `git tag 2.1.0-ZOLA`) and
ensure the working tree is clean before publishing.

### Cross-publish command

The plugin cross-builds two variants — Scala 2.12 / sbt 1.x and Scala
3 / sbt 2.x. Use the `+` prefix to publish both, and set
`ZOLA_PUBLISH=1` so `publishZola.sbt` switches `organization` from
upstream's `com.github.sbt` to `com.zola.sbt`:

```
ZOLA_PUBLISH=1 sbt "+clean" "+publish"
```

(Without the env var, `organization` stays at `com.github.sbt` so
`sbt scripted` continues to resolve the locally-published plugin
correctly.)

This produces:

| Scala   | sbt API    | Coordinate suffix |
| ------- | ---------- | ----------------- |
| 2.12.x  | 1.x        | `_2.12_1.0`       |
| 3.x     | 2.0.0-RC11 | `_3_2.0`          |

Coordinates: `com.zola.sbt:sbt-less_<scala>_<sbt>:<version>`.

> **Do not run `sbt ci-release`.** That task is wired for Sonatype
> Central (GPG signing, staging, close-and-promote) and is not
> appropriate for the Zola Nexus flow.
