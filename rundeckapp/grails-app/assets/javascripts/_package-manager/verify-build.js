// verify-build.js
//
// Automated regression check for build.js. Asserts that every expected
// vendor asset was produced under ../vendor, that each is non-empty, and
// that the one non-trivial case -- the bootstrap.js concatenation -- still
// contains recognizable content from both of its source files, in order.
// This is meant to be run immediately after build.js (see the "test",
// "dev:build" and "ci:build" npm scripts), so it catches a future change to
// build.js -- or a vendor package's file layout -- that silently breaks
// the packaged output.

const assert = require('assert');
const fs = require('fs');
const path = require('path');

const vendorDir = path.resolve(__dirname, '../vendor');

/**
 * Simple copy outputs expected under ../vendor, relative to vendorDir.
 * @type {string[]}
 */
const expectedFiles = [
    'jquery.js',
    'jquery-ui.js',
    'jquery-ui.css',
    'jquery-ui-timepicker-addon.js',
    'knockout.min.js',
    'jquery.autocomplete.min.js',
    'knockout-mapping.js',
    'perfect-scrollbar.js',
    'perfect-scrollbar.css',
    'vue.global.js',
    'vue.global.prod.js',
    path.join('bootstrap', 'bootstrap.js'),
];

/**
 * Asserts that a file exists under vendorDir and is non-empty.
 *
 * @param {string} relativePath vendor-relative file path
 */
function assertNonEmptyFile(relativePath) {
    const fullPath = path.join(vendorDir, relativePath);
    assert.ok(
        fs.existsSync(fullPath),
        `Expected vendor output missing: ${relativePath} (looked in ${fullPath}). Did build.js run, or did a source path change?`
    );
    const {size} = fs.statSync(fullPath);
    assert.ok(size > 0, `Expected vendor output is empty: ${relativePath}`);
}

/**
 * Verifies that the combined bootstrap.js output actually contains content
 * from both source files, in the expected order, so a future change that
 * breaks the concatenation logic is caught rather than passing a mere
 * "file exists" check.
 */
function verifyBootstrapConcatenation() {
    const bootstrapSource = fs.readFileSync(path.resolve(__dirname, '../bootstrap_3_4_2.js'), 'utf8');
    const popoverSource = fs.readFileSync(path.resolve(__dirname, './popover-default.js'), 'utf8');
    const combined = fs.readFileSync(path.join(vendorDir, 'bootstrap', 'bootstrap.js'), 'utf8');

    // Distinctive substrings from each source, rather than the whole file,
    // so this remains a meaningful smoke test even if the sources change.
    const bootstrapMarker = bootstrapSource.trim().slice(0, 80);
    const popoverMarker = popoverSource.trim().slice(0, 80);

    assert.ok(
        combined.includes(bootstrapMarker),
        'bootstrap.js output does not contain content from bootstrap_3_4_2.js'
    );
    assert.ok(
        combined.includes(popoverMarker),
        'bootstrap.js output does not contain content from popover-default.js'
    );
    assert.ok(
        combined.indexOf(bootstrapMarker) < combined.indexOf(popoverMarker),
        'bootstrap.js output has bootstrap_3_4_2.js and popover-default.js content in the wrong order'
    );
}

function main() {
    assert.ok(fs.existsSync(vendorDir), `Vendor output directory missing: ${vendorDir}. Did build.js run?`);

    expectedFiles.forEach(assertNonEmptyFile);
    verifyBootstrapConcatenation();

    console.log(`verify-build: OK (${expectedFiles.length} vendor files verified)`);
}

try {
    main();
} catch (err) {
    console.error(`verify-build: FAILED - ${err.message}`);
    process.exit(1);
}
