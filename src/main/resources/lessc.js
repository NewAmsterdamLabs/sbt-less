/*global process, require */

(function () {

    "use strict";

    var requireIfExists = require('node-require-fallback')

    // Ensure we have a promise implementation.
    //
    if (typeof Promise === 'undefined') {
        var Promise = requireIfExists("es6-promise/4.2.8", "es6-promise").Promise; // sync with build.sbt
        global.Promise = Promise;
    }

    var args = process.argv,
        os = require("os"),
        fs = require("fs"),
        less = requireIfExists("less/4.6.3", "less"), // sync with build.sbt
        mkdirp = requireIfExists("mkdirp/0.5.6", "mkdirp"), // sync with build.sbt
        path = require("path");

    var SOURCE_FILE_MAPPINGS_ARG = 2;
    var TARGET_ARG = 3;
    var OPTIONS_ARG = 4;

    var sourceFileMappings = JSON.parse(args[SOURCE_FILE_MAPPINGS_ARG]);
    var target = args[TARGET_ARG];
    var optionsString = args[OPTIONS_ARG];

    var sourcesToProcess = sourceFileMappings.length;
    var results = [];
    var problems = [];

    function parseDone() {
        if (--sourcesToProcess === 0) {
            console.log("\u0010" + JSON.stringify({results: results, problems: problems}));
        }
    }

    function throwIfErr(e) {
        if (e) throw e;
    }

    // Build the clean-css post-processor plugin once, outside the per-file loop. The
    // options are identical for every source file (parsed from the same jsOptions JSON),
    // and the plugin object holds no per-file state — it only closes over cleanCssOpts.
    var baseOptions = JSON.parse(optionsString);
    var cleanCssPlugin = null;
    if (baseOptions.cleancss) {
        var CleanCSS = requireIfExists("clean-css/5.3.3", "clean-css"); // sync with build.sbt
        var cleanCssOpts = baseOptions.cleancssOptions || {};
        cleanCssPlugin = {
            install: function (less, pluginManager) {
                pluginManager.addPostProcessor({
                    process: function (css, extra) {
                        var ccOpts = Object.assign({}, cleanCssOpts);
                        var originalMap = null;
                        if (extra.sourceMap) {
                            var externalMap = extra.sourceMap.getExternalSourceMap();
                            if (externalMap) {
                                var externalMapStr = externalMap.toString();
                                originalMap = JSON.parse(externalMapStr);
                                ccOpts.sourceMap = externalMapStr;
                            }
                        }
                        var ccResult = new CleanCSS(ccOpts).minify(css);
                        // clean-css uses `errors` for both fatal and non-fatal issues (e.g.
                        // missing @import targets are reported here but clean-css still
                        // produces valid styles). Only fail when no styles were produced.
                        if (ccResult.styles === undefined) {
                            throw new Error("clean-css produced no output" +
                                (ccResult.errors && ccResult.errors.length
                                    ? ": " + ccResult.errors.join("; ")
                                    : ""));
                        }
                        if (extra.sourceMap && ccResult.sourceMap) {
                            // clean-css names the anonymous input "$stdin" in the merged map; restore
                            // the original `sources` and `sourcesContent` from less.js's map so the
                            // final .css.map references the actual .less file(s).
                            var mergedMap = JSON.parse(String(ccResult.sourceMap));
                            if (originalMap) {
                                if (originalMap.sources) mergedMap.sources = originalMap.sources;
                                if (originalMap.sourcesContent) mergedMap.sourcesContent = originalMap.sourcesContent;
                            }
                            extra.sourceMap.setExternalSourceMap(JSON.stringify(mergedMap));
                        }
                        return ccResult.styles + (extra.sourceMap ? extra.sourceMap.getCSSAppendage() : "");
                    }
                });
            }
        };
    }

    sourceFileMappings.forEach(function (sourceFileMapping) {

        // Reparse options each time so we get a different object that can be modified
        var options = JSON.parse(optionsString);

        var input = sourceFileMapping[0];
        var outputFile = sourceFileMapping[1].replace(".less", options.compress ? ".min.css" : ".css");
        var output = path.join(target, outputFile);
        var sourceMapOutput = output + ".map";

        // sourceMapFileInline makes less.js embed the map inline — skip the external .map file in that case.
        var writeExternalSourceMap = (options.sourceMap == true) && !options.sourceMapFileInline;

        options.sourceMap = options.sourceMap == true ? {
            sourceMapBasepath: path.dirname(input),
            sourceMapFullFilename: path.basename(sourceMapOutput),
            sourceMapOutputFilename: path.basename(outputFile),
            sourceMapFileInline: options.sourceMapFileInline || false,
            outputSourceFiles: options.sourceMapLessInline || false,
            sourceMapRootpath: options.sourceMapRootpath || ""
        } : null;
        options.filename = input;

        options.plugins = cleanCssPlugin ? [cleanCssPlugin] : [];

        var writeSourceMap = function (content, onDone) {
            if (content && writeExternalSourceMap) { // NOTE: content check is a workaround for https://github.com/less/less.js/issues/2430
                if (options.relativeImports) {
                    // replace leading part in included assets with "../"
                    content = JSON.parse(content);
                    for (var i = 0, s = content.sources, l = s.length; i < l; i++) {
                        options.paths.forEach(function(path) {
                            // for windows replace \ with /
                            path = path.replace(/\\/g, "/");
                            if (path[path.length - 1] !== "/")
                                path += "/";
                            if (s[i].substr(0, path.length) === path)
                                s[i] = "../" + s[i].substr(path.length);
                        });
                    }
                    content = JSON.stringify(content);
                }

                mkdirp(path.dirname(sourceMapOutput), function (e) {
                    throwIfErr(e);
                    fs.writeFile(sourceMapOutput, content, "utf8", onDone);
                });
            } else {
              onDone()
            }
        };


        var writeOutput = function (content, onDone) {
            mkdirp(path.dirname(output), function (e) {
                throwIfErr(e);
                fs.writeFile(output, content, "utf8", onDone);
            });
        };

        var handleResult = function (result) {
            writeOutput(result.css, function (e) {
                throwIfErr(e);

                writeSourceMap(result.map, function (e) {
                    throwIfErr(e);

                    var imports = [];
                    for (var i = 0; i < result.imports.length; i++) {
                        imports.push(result.imports[i]);
                    }

                    results.push({
                        source: input,
                        result: {
                            filesRead: [input].concat(imports),
                            filesWritten: writeExternalSourceMap ? [output, sourceMapOutput] : [output]
                        }
                    });

                    parseDone();
                });
            });
        };

        fs.readFile(input, "utf8", function (e, content) {
            throwIfErr(e);


            function handleLessError(e) {
                if (e.line != undefined && e.column != undefined) {
                    problems.push({
                        message: e.message,
                        severity: "error",
                        lineNumber: e.line,
                        characterOffset: e.column,
                        lineContent: content.split("\n")[e.line - 1],
                        source: input
                    });
                } else {
                    throw e;
                }
                results.push({
                    source: input,
                    result: null
                });

                parseDone();
            }

            less.render(content, options)
                .then(handleResult, handleLessError);
        });
    });
})();