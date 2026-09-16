// test-fileops.js
//
// Fast, isolated unit tests for lib/fileOps.js's copy() and combine(), using
// synthetic fixture files in a temp directory -- independent of any real
// vendor package under node_modules. Plain Node assert/fs, no test
// framework, same style as verify-build.js. Meant to run before the
// slower, real-vendor-package end-to-end check (see the "test" npm script).

const assert = require('assert');
const fs = require('fs');
const os = require('os');
const path = require('path');
const {copy, combine} = require('./lib/fileOps');

/**
 * Creates a fresh temp directory for a single test case.
 * @returns {string} absolute path to the new temp directory
 */
function makeTempDir() {
    return fs.mkdtempSync(path.join(os.tmpdir(), 'package-manager-fileops-test-'));
}

function testCopyCreatesDestinationDirAndMatchesContent() {
    const tmp = makeTempDir();
    try {
        const srcPath = path.join(tmp, 'source.txt');
        const destPath = path.join(tmp, 'nested', 'dest', 'copied.txt');
        const content = 'fixture content for copy()';
        fs.writeFileSync(srcPath, content);

        assert.ok(!fs.existsSync(path.dirname(destPath)), 'precondition: destination dir should not exist yet');

        copy(srcPath, destPath);

        assert.ok(fs.existsSync(destPath), 'copy() did not create the destination file');
        assert.strictEqual(
            fs.readFileSync(destPath, 'utf8'),
            content,
            'copy() destination content does not exactly match source content'
        );
    } finally {
        fs.rmSync(tmp, {recursive: true, force: true});
    }
}

function testCombineJoinsFilesInOrderWithNewline() {
    const tmp = makeTempDir();
    try {
        const firstPath = path.join(tmp, 'first.js');
        const secondPath = path.join(tmp, 'second.js');
        const destPath = path.join(tmp, 'nested', 'dest', 'combined.js');
        fs.writeFileSync(firstPath, 'first-fixture');
        fs.writeFileSync(secondPath, 'second-fixture');

        assert.ok(!fs.existsSync(path.dirname(destPath)), 'precondition: destination dir should not exist yet');

        combine([firstPath, secondPath], destPath);

        assert.ok(fs.existsSync(destPath), 'combine() did not create the destination file');
        assert.strictEqual(
            fs.readFileSync(destPath, 'utf8'),
            'first-fixture\nsecond-fixture',
            'combine() did not join sources in order with a newline separator'
        );
    } finally {
        fs.rmSync(tmp, {recursive: true, force: true});
    }
}

const tests = [
    ['copy() creates destination dir and matches source content', testCopyCreatesDestinationDirAndMatchesContent],
    ['combine() joins files in order with newline', testCombineJoinsFilesInOrderWithNewline],
];

let failures = 0;
for (const [name, run] of tests) {
    try {
        run();
        console.log(`test-fileops: OK - ${name}`);
    } catch (err) {
        failures += 1;
        console.error(`test-fileops: FAILED - ${name}: ${err.message}`);
    }
}

if (failures > 0) {
    console.error(`test-fileops: ${failures} of ${tests.length} test(s) failed`);
    process.exit(1);
}
console.log(`test-fileops: all ${tests.length} test(s) passed`);
