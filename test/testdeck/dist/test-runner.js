"use strict";
var __importDefault = (this && this.__importDefault) || function (mod) {
    return (mod && mod.__esModule) ? mod : { "default": mod };
};
Object.defineProperty(exports, "__esModule", { value: true });
exports.BitScriptRunner = void 0;
const chalk_1 = __importDefault(require("chalk"));
const indent_string_1 = __importDefault(require("indent-string"));
const child_process_1 = require("./async/child-process");
class BitScriptRunner {
    constructor(testRepo) {
        this.testRepo = testRepo;
    }
    async run() {
        const groupResults = await this.testGroups();
        this.summary(groupResults);
    }
    async testGroups() {
        const groupResults = [];
        for (const group of this.testRepo.groups) {
            const testResults = [];
            for (const test of group.tests) {
                let stdout = '';
                let stderr = '';
                let success = false;
                try {
                    const res = await (0, child_process_1.exec)(test.file);
                    console.log(`${chalk_1.default.green('✔️')} ${group.name}/${test.name}`);
                    stdout = res.stdout;
                    stderr = res.stderr;
                    success = true;
                }
                catch (e) {
                    console.log(`${chalk_1.default.red('❌')} ${test.name} ${test.file}`);
                    stdout = e.stdout;
                    stderr = e.stderr;
                }
                finally {
                    if (stdout != '')
                        console.log((0, indent_string_1.default)(`stdout:\n${(0, indent_string_1.default)(stdout, 4)}`, 4));
                    if (stderr != '')
                        console.log((0, indent_string_1.default)(`stderr:\n${(0, indent_string_1.default)(chalk_1.default.red(stderr), 4)}`, 4));
                }
                testResults.push({
                    stderr,
                    stdout,
                    success,
                    test,
                });
            }
            groupResults.push({
                testGroup: group,
                testResults,
            });
        }
        return groupResults;
    }
    summary(resultGroups) {
        let total = 0;
        let passed = 0;
        let failed = 0;
        const testResults = resultGroups.reduce((tests, group) => tests.concat(group.testResults), []);
        testResults.forEach(result => {
            if (result.success)
                passed++;
            else
                failed++;
            total++;
        });
        const metrics = [];
        if (failed != 0)
            metrics.push(chalk_1.default.red(`${failed} failed`));
        if (passed != 0)
            metrics.push(chalk_1.default.green(`${passed} passed`));
        metrics.push(`${total} total`);
        let statusEmoji = '';
        if (failed == 0)
            statusEmoji = chalk_1.default.green('💯');
        else
            statusEmoji = chalk_1.default.red('⛔');
        console.log(`${statusEmoji} Tests: ${metrics.join(', ')}`);
        if (failed != 0)
            process.exitCode = 1;
    }
}
exports.BitScriptRunner = BitScriptRunner;
//# sourceMappingURL=test-runner.js.map