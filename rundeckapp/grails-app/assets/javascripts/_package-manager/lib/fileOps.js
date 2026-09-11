// lib/fileOps.js
//
// The two primitive file operations that build.js needs -- a plain copy and
// a plain concatenation -- extracted into a standalone, requirable module so
// they can be unit tested in isolation (see test-fileops.js) without
// touching the real vendor packages under node_modules. Both functions take
// already-resolved paths; callers (build.js) are responsible for resolving
// relative paths against their own __dirname before calling in.

const fs = require('fs');
const path = require('path');

/**
 * Copies a single file from src to dest, creating the destination
 * directory first if it does not already exist.
 *
 * @param {string} srcPath absolute (or cwd-relative) path to the source file
 * @param {string} destPath absolute (or cwd-relative) path to the destination file
 */
function copy(srcPath, destPath) {
    fs.mkdirSync(path.dirname(destPath), {recursive: true});
    fs.copyFileSync(srcPath, destPath);
}

/**
 * Concatenates the contents of multiple source files, in the given order,
 * joining them with a newline, and writes the result to a single
 * destination file, creating the destination directory first if it does
 * not already exist.
 *
 * @param {string[]} sourcePaths absolute (or cwd-relative) paths to the
 *     source files, in concatenation order
 * @param {string} destPath absolute (or cwd-relative) path to the destination file
 */
function combine(sourcePaths, destPath) {
    fs.mkdirSync(path.dirname(destPath), {recursive: true});
    const combined = sourcePaths.map((src) => fs.readFileSync(src)).join('\n');
    fs.writeFileSync(destPath, combined);
}

module.exports = {copy, combine};
