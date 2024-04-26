"use strict";
var __importDefault = (this && this.__importDefault) || function (mod) {
    return (mod && mod.__esModule) ? mod : { "default": mod };
};
Object.defineProperty(exports, "__esModule", { value: true });
exports.ShimApiTests = void 0;
const child_process_1 = __importDefault(require("child_process"));
const fs_1 = __importDefault(require("fs"));
const rundeck_1 = require("../test/rundeck");
const api_1 = require("../test/api");
const skipTests = [
    'test-job-run-steps.sh',
    'test-job-run-webhook.sh',
    'test-job-run-without-deadlock.sh',
    'test-execution-cleaner-job.sh',
    'test-execution-output-plain-lastlines.sh',
    'test-execution-output-plain.sh',
    'test-execution-output-utf8.sh',
    'test-execution-state.sh',
    '^test-scm',
    /** Misc */
    'test-history.sh',
    'test-metrics.sh',
    'test-require-version.sh',
    'test-resource.sh',
    'test-resources.sh',
    'test-run-script-interpreter.sh',
    'test-run-script.sh',
    'test-v23-project-source-resources.sh',
    'test-v23-project-sources-json.sh',
    'test-v23-project-sources-xml.sh',
    'test-workflow-errorhandler.sh',
];
function ShimApiTests(pattern) {
    (0, api_1.CreateTestContext)({ projects: ['test'] });
    beforeAll(async () => {
        const out = child_process_1.default.execSync(`RDECK_URL=${rundeck_1.envOpts.TESTDECK_RUNDECK_URL} SHELL=/bin/bash bash ./rundecklogin.sh - admin admin`, { cwd: '../api' });
    });
    let tests = fs_1.default.readdirSync('../api');
    tests = tests.filter(t => pattern.test(t) && t.endsWith('.sh'));
    for (let t of tests) {
        if (skipTests.some(s => new RegExp(s).test(t))) {
            console.log(`Skipping ${t}`);
            it.skip(t, () => { });
            continue;
        }
        it(t, () => {
            try {
                const out = child_process_1.default.execSync(`RDECK_URL=${rundeck_1.envOpts.TESTDECK_RUNDECK_URL} SHELL=/bin/bash bash ./${t} -`, { cwd: '../api' });
            }
            catch (e) {
                const ex = e;
                ex.message = `${e.stdout.toString()}\n${ex.message}`;
                throw e;
            }
        });
    }
}
exports.ShimApiTests = ShimApiTests;
//# sourceMappingURL=apiShim.js.map