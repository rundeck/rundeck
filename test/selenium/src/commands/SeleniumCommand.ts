import {Argv} from 'yargs'

import {spawn} from 'async/child-process'
import { Rundeck, PasswordCredentialProvider } from 'ts-rundeck';
import { ProjectImporter } from 'projectImporter';
import { sleep } from 'async/util';

interface Opts {
    debug: boolean
    url: string
    jest: string
    headless: boolean
    s3Upload: boolean
    s3Base: string
}

class SeleniumCommand {
    command = "selenium"
    describe = "Run selenium test suite"

    builder(yargs: Argv) {
        return yargs
            .option("u", {
                alias: "url",
                default: `http://${process.env.HOSTNAME}:4440`,
                describe: "Rundeck URL"
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
            })
    }

    async handler(opts: Opts) {
        let args: string
        if (opts.debug)
            args = `node --inspect-brk ./node_modules/.bin/jest --runInBand ${opts.jest}`
        else
            args = `node ./node_modules/.bin/jest ${opts.jest}`

        const client = new Rundeck(new PasswordCredentialProvider(opts.url, 'admin', 'admin'), opts.url)

        await waitForRundeckReady(client)

        const importer = new ProjectImporter('./lib', 'SeleniumBasic', client)
        await importer.importProject()

        const ret = await spawn('/bin/sh', ['-c', args], {
            stdio: 'inherit',
            env: {
                ...process.env,
                SELENIUM_PROMISE_MANAGER: '0',
                RUNDECK_URL: opts.url,
                HEADLESS: opts.headless.toString(),
                S3_UPLOAD: opts.s3Upload.toString(),
                S3_BASE: opts.s3Base,
            }})
        if (ret != 0)
            process.exitCode = 1
    }
}

async function waitForRundeckReady(client: Rundeck, timeout = 120000) {
    const start = Date.now()
    while (Date.now() - start < timeout) {
        try {
            await client.systemInfoGet()
            return
        } catch  (e) {
            await sleep(5000)
        }
    }
    throw new Error('Timeout exceeded waiting for Rundeck to be ready.')
}

module.exports = new SeleniumCommand()
