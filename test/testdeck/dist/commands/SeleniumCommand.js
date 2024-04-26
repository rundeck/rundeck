"use strict";
Object.defineProperty(exports, "__esModule", { value: true });
const ts_rundeck_1 = require("@rundeck/client");
const child_process_1 = require("../async/child-process");
const projectImporter_1 = require("../projectImporter");
const util_1 = require("../async/util");
class SeleniumCommand {
    constructor() {
        this.command = "selenium";
        this.describe = "Run selenium test suite";
    }
    builder(yargs) {
        return yargs
            .option("u", {
            alias: "url",
            default: `http://${process.env.HOSTNAME}:4440`,
            describe: "Rundeck URL"
        })
            .option('t', {
            alias: 'testToken',
            describe: 'API Token to use for tests',
            type: 'string'
        })
            .option("j", {
            alias: "jest",
            describe: "Jest args",
            type: 'string',
            default: ''
        })
            .option("s", {
            alias: "suite",
            describe: "Sub suite of selenium tests to run",
            type: 'array',
            choices: ['all', 'functional', 'visual-regression']
        })
            .option("h", {
            alias: "headless",
            describe: "Run Chrome in headless mode",
            type: 'boolean',
            default: false
        })
            .option('s3-upload', {
            describe: 'Upload to s3; credentials must be available',
            type: 'boolean'
        })
            .option('s3-base', {
            describe: 'Base path for uploading artifacts',
            type: 'string',
            default: 'projects/rundeck/images/selenium'
        })
            .option('debug', {
            describe: 'Debug node process',
            type: 'boolean',
            default: false
        });
    }
    async handler(opts) {
        let args;
        if (opts.debug)
            args = `node --inspect-brk ./node_modules/.bin/jest --testPathPattern="__tests__\/selenium\/" --runInBand ${opts.jest}`;
        else
            args = `node ./node_modules/.bin/jest --testPathPattern="__tests__\/selenium\/" ${opts.jest}`;
        const client = new ts_rundeck_1.Rundeck(opts.testToken ? new ts_rundeck_1.TokenCredentialProvider(opts.testToken) : new ts_rundeck_1.PasswordCredentialProvider(opts.url, 'admin', 'admin'), { baseUri: opts.url });
        await waitForRundeckReady(client);
        const importer = new projectImporter_1.ProjectImporter('./lib/projects', 'SeleniumBasic', client);
        await importer.importProject();
        const ret = await (0, child_process_1.spawn)('/bin/sh', ['-c', args], {
            stdio: 'inherit',
            env: Object.assign(Object.assign({}, process.env), { SELENIUM_PROMISE_MANAGER: '0', RUNDECK_URL: opts.url, RUNDECK_TOKEN: opts.testToken, HEADLESS: opts.headless.toString(), S3_UPLOAD: opts.s3Upload.toString(), S3_BASE: opts.s3Base })
        });
        if (ret != 0)
            process.exitCode = 1;
    }
}
async function waitForRundeckReady(client, timeout = 120000) {
    const start = Date.now();
    while (Date.now() - start < timeout) {
        try {
            await client.systemInfoGet();
            return;
        }
        catch (e) {
            await (0, util_1.sleep)(5000);
        }
    }
    throw new Error('Timeout exceeded waiting for Rundeck to be ready.');
}
module.exports = new SeleniumCommand();
//# sourceMappingURL=SeleniumCommand.js.map