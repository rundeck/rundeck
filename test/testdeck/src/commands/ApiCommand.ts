import {Argv} from 'yargs'

import {spawn} from '../async/child-process'
import { Rundeck, PasswordCredentialProvider } from 'ts-rundeck'

import {waitForRundeckReady} from '../util/RundeckAPI'

interface Opts {
    debug: boolean
    url: string
    jest: string
    headless: boolean
    s3Upload: boolean
    s3Base: string
    testPath: string
}

class ApiCommand {
    command = "api"
    describe = "Run api test suite"

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
            .option('testPath', {
                type: 'string'
            })
            .option('testNamePattern', {
                type: 'string'
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
        let args: string
        if (opts.debug)
            args = `node --inspect-brk ./node_modules/.bin/jest --runInBand --testPathPattern="__tests__\/api\/" ${opts.jest}`
        else
            args = `node ./node_modules/.bin/jest --testPathPattern="__tests__\/api\/${opts.testPath || ''}" ${opts.jest}`

        console.log(opts)

        const client = new Rundeck(new PasswordCredentialProvider(opts.url, 'admin', 'admin'), {baseUri: opts.url})

        await waitForRundeckReady(client)

        const projects = await client.projectList()

        const testProjectPrefixes = ['project-', 'API', 'APITest', 'scheduler-', 'testscm']

        const testProjects = projects.filter(project => {
            return testProjectPrefixes.some(prefix => {
                return project.name.startsWith(prefix)
            })
        })

        if (! testProjects.find(p => p.name == 'test'))
            await client.projectCreate({name: 'test'})

        console.log(testProjects)

        let jobs = await client.jobList('test')

        for (let job of jobs) {
            await client.jobDelete(job.id)
        }

        const cleanupProms = testProjects.map(p => client.projectDelete(p.name!))

        await Promise.all(cleanupProms)

        await client.projectCreate({name: 'test'})

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

module.exports = new ApiCommand()
