// build.js
//
// Plain Node.js replacement for the former laravel-mix/webpack build
// (webpack.mix.js). This directory only ever needed file copies and a
// single two-file concatenation to stage third-party vendor assets under
// ../vendor -- no transpilation or bundling was ever performed here, so a
// small script using Node's built-in `fs`/`path` modules replaces the
// entire laravel-mix/webpack toolchain with no new dependencies.

const fs = require('fs');
const path = require('path');

/**
 * Copies a single file from source to destination, creating the
 * destination directory if it does not already exist.
 *
 * @param {string} src relative path to the source file
 * @param {string} dest relative path to the destination file
 */
function copy(src, dest) {
    const srcPath = path.resolve(__dirname, src);
    const destPath = path.resolve(__dirname, dest);
    fs.mkdirSync(path.dirname(destPath), {recursive: true});
    fs.copyFileSync(srcPath, destPath);
}

/**
 * Concatenates the contents of multiple source files, in order, and writes
 * the result to a single destination file, creating the destination
 * directory if it does not already exist.
 *
 * @param {string[]} sources relative paths to the source files, in
 *     concatenation order
 * @param {string} dest relative path to the destination file
 */
function combine(sources, dest) {
    const destPath = path.resolve(__dirname, dest);
    fs.mkdirSync(path.dirname(destPath), {recursive: true});
    const combined = sources
        .map((src) => fs.readFileSync(path.resolve(__dirname, src)))
        .join('\n');
    fs.writeFileSync(destPath, combined);
}

copy('node_modules/jquery/dist/jquery.min.js', '../vendor/jquery.js');
copy('node_modules/jquery-ui-dist/jquery-ui.min.js', '../vendor/jquery-ui.js');
copy('node_modules/jquery-ui-dist/jquery-ui.css', '../vendor/jquery-ui.css');
copy(
    'node_modules/jquery-ui-timepicker-addon/dist/jquery-ui-timepicker-addon.min.js',
    '../vendor/jquery-ui-timepicker-addon.js'
);
copy('../knockout_3_5_1.js', '../vendor/knockout.min.js');
copy('../jquery_autocomplete_1_3_0.js', '../vendor/jquery.autocomplete.min.js');
copy('node_modules/knockout-mapping/dist/knockout.mapping.min.js', '../vendor/knockout-mapping.js');
copy('node_modules/perfect-scrollbar/dist/perfect-scrollbar.min.js', '../vendor/perfect-scrollbar.js');
copy('node_modules/perfect-scrollbar/css/perfect-scrollbar.css', '../vendor/perfect-scrollbar.css');
copy('node_modules/vue/dist/vue.global.js', '../vendor/vue.global.js');
copy('node_modules/vue/dist/vue.global.prod.js', '../vendor/vue.global.prod.js');
combine(['../bootstrap_3_4_2.js', './popover-default.js'], '../vendor/bootstrap/bootstrap.js');

console.log('Vendor assets built successfully.');
