"use strict";
Object.defineProperty(exports, "__esModule", { value: true });
const ts_rundeck_1 = require("@rundeck/client");
const child_process_1 = require("../async/child-process");
const RundeckAPI_1 = require("../util/RundeckAPI");
const ClusterManager_1 = require("../ClusterManager");
const Config_1 = require("../Config");
const DEFAULT_DOCKER_COMPOSE_FILE_NAME = 'docker-compose.yml';
class TestCommand {
    constructor() {
        this.command = "test";
        this.describe = "Run test suites";
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
            .option('c', {
            alias: 'clusterConfig',
            describe: 'Directory containing cluster configuration for test',
            type: 'string'
        })
            .option('f', {
            alias: 'composeFileName',
            describe: 'Allows for a non-standard Docker Compose file name',
            type: 'string'
        })
            .option('provision', {
            describe: 'Provision a cluster to run tests against',
            type: 'boolean',
            default: false
        })
            .option('down', {
            describe: 'Shutdown cluster after tests complete',
            type: 'boolean',
            default: true,
        })
            .option('image', {
            describe: 'The Rundeck Docker image to use instead of the default',
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
            type: 'string',
        })
            .option("n", {
            alias: "testName",
            describe: "Pattern to match test-name",
            type: 'string'
        })
            .option("h", {
            alias: "headless",
            describe: "Run Chrome in headless mode",
            type: 'boolean',
            default: false
        })
            .option('runInBand', {
            describe: 'Run jest inband',
            type: 'boolean',
            default: true
        })
            .option('watch', {
            describe: 'Run Jest in watch mode',
            type: 'boolean',
            default: false
        })
            .option('s3-upload', {
            describe: 'Upload to s3; credentials must be available',
            type: 'boolean',
            default: false
        })
            .option('s3-base', {
            describe: 'Base path for uploading artifacts',
            type: 'string',
            default: 'projects/rundeck/images/selenium'
        })
            .option('visualRegression', {
            describe: 'Process screenshots for visual regression',
            type: 'boolean',
            default: false
        })
            .option('debug', {
            describe: 'Debug node process',
            type: 'boolean',
            default: false
        });
    }
    async handler(opts) {
        const config = await Config_1.Config.Load('./config.yml', './config.user.yml');
        let cluster;
        if (opts.provision) {
            cluster = await ClusterManager_1.ClusterFactory.CreateCluster(opts.clusterConfig || config.clusterConfig, {
                licenseFile: './license.key',
                image: opts.image || config.baseImage,
                composeFileName: opts.composeFileName || DEFAULT_DOCKER_COMPOSE_FILE_NAME
            });
            await cluster.startCluster();
            process.on('SIGINT', async () => {
                if (opts.down) {
                    if (cluster) {
                        console.log('Shutting down...');
                        await cluster.stopCluster();
                    }
                }
                process.exit();
            });
        }
        const cmdArgs = [
            'node',
            opts.debug ? '--inspect-brk' : null,
            './node_modules/.bin/jest',
            (opts.debug || opts.runInBand) ? '--runInBand' : null,
            opts.suite ? `--testPathPattern='__tests__/${opts.suite}'` : null,
            opts.testName ? `--testNamePattern="${opts.testName}"` : null,
            opts.watch ? '--watch' : null,
            opts.jest
        ];
        /** Drop the null entries and construct command string */
        const cmdString = cmdArgs.filter(a => a != null).join(' ');
        console.info(`Waiting for server to accept requests...`);
        const reqstart = Date.now();
        await (0, RundeckAPI_1.createWaitForRundeckReady)(() => new ts_rundeck_1.Rundeck(opts.testToken ? new ts_rundeck_1.TokenCredentialProvider(opts.testToken) : new ts_rundeck_1.PasswordCredentialProvider(opts.url, 'admin', 'admin'), { noRetryPolicy: true, baseUri: opts.url }), 5 * 60 * 1000);
        console.info(`Client connected. (${Date.now() - reqstart}ms)`);
        console.log(cmdString);
        const ret = await (0, child_process_1.spawn)('/bin/sh', ['-c', cmdString], {
            stdio: 'inherit',
            env: Object.assign(Object.assign({}, process.env), { SELENIUM_PROMISE_MANAGER: '0', TESTDECK_RUNDECK_URL: opts.url, TESTDECK_RUNDECK_TOKEN: opts.testToken, TESTDECK_HEADLESS: opts.headless.toString(), TESTDECK_S3_UPLOAD: opts.s3Upload.toString(), TESTDECK_S3_BASE: opts.s3Base, TESTDECK_CLUSTER_CONFIG: opts.clusterConfig || config.clusterConfig, TESTDECK_BASE_IMAGE: opts.image || config.baseImage, TESTDECK_VISUAL_REGRESSION: opts.visualRegression.toString() })
        });
        if (ret != 0)
            process.exitCode = 1;
        if (opts.provision && opts.down && cluster)
            await cluster.stopCluster();
    }
}
module.exports = new TestCommand();
//# sourceMappingURL=TestCommand.js.map