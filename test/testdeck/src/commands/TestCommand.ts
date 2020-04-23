import {Argv} from 'yargs'

import { Rundeck, PasswordCredentialProvider } from 'ts-rundeck'

import {spawn} from '../async/child-process'
import { ProjectImporter } from '../projectImporter'
import { waitForRundeckReady } from '../util/RundeckAPI'
import { ClusterFactory, IClusterManager } from '../ClusterManager'
import { Config } from '../Config';

interface Opts {
    provision: boolean
    clusterConfig?: string
    image?: string
    debug: boolean
    url: string
    jest: string
    headless: boolean
    runInBand: boolean
    s3Upload: boolean
    s3Base: string
    suite?: string
    testName?: string
    watch: boolean
}

class TestCommand {
    command = "test"
    describe = "Run test suites"

    builder(yargs: Argv) {
        return yargs
            .option("u", {
                alias: "url",
                default: `http://${process.env.HOSTNAME}:4440`,
                describe: "Rundeck URL"
            })
            .option('c', {
                alias: 'clusterConfig',
                describe: 'Directory containing cluster configuration for test',
                type: 'string'
            })
            .option('provision', {
                describe: 'Provision a cluster to run tests against',
                type: 'boolean',
                default: false
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
                default:true
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
            .option('debug', {
                describe: 'Debug node process',
                type: 'boolean',
                default: false
            })
    }

    async handler(opts: Opts) {
        const config = await Config.Load('./config.yml')

        let cluster: IClusterManager
        if (opts.provision) {
            cluster = await ClusterFactory.CreateCluster(opts.clusterConfig || config.clusterConfig, {
                licenseFile: './license.key',
                image: opts.image || config.baseImage
            })

            await cluster.startCluster()

            process.on('SIGINT', async () => {
                console.log('Shutting down...')
                await cluster.stopCluster()
                process.exit()
            })
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
        ]

        /** Drop the null entries and construct command string */
        const cmdString = cmdArgs.filter(a => a != null).join(' ')

        console.log(cmdString)

        const client = new Rundeck(new PasswordCredentialProvider(opts.url, 'admin', 'admin'), {baseUri: opts.url})

        await waitForRundeckReady(client)

        const importer = new ProjectImporter('./lib/projects', 'SeleniumBasic', client)
        await importer.importProject()

        const ret = await spawn('/bin/sh', ['-c', cmdString], {
            stdio: 'inherit',
            env: {
                ...process.env,
                SELENIUM_PROMISE_MANAGER: '0',
                RUNDECK_URL: opts.url,
                HEADLESS: opts.headless.toString(),
                S3_UPLOAD: opts.s3Upload.toString(),
                S3_BASE: opts.s3Base,
                TESTDECK_CLUSTER_CONFIG: opts.clusterConfig || config.clusterConfig,
            }})

        if (ret != 0)
            process.exitCode = 1

        if (opts.provision)
            await cluster.stopCluster()

    }
}

module.exports = new TestCommand()
