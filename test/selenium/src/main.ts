#!/usr/bin/env ts-node

import * as yargs from 'yargs' // Yargs matey!

import { spawn } from './async/child-process'
import {EnvBuilder} from './env-builder'
import {Client} from './rundeck-client/client'
import {TestRepo} from './test-repo'
import {BitScriptRunner} from './test-runner'

const DEFAULT_RD_URL = 'http://localhost:8080'
const DEFAULT_RD_USER = 'admin'
const DEFAULT_RD_PASSWORD = 'admin'

interface IArgv {
    filter: string
    jest: string
    runner: 'bitscript' | 'jest'
    teardown: boolean
}

const argv = yargs
    .option('f', {
        alias: 'filter',
        default: '.*',
        description: 'Regexp pattern to filter full test path against'})
    .option('j', {alias: 'jest', type: 'string', default: '', description: 'Options to pass to Jest'})
    .option('r', {alias: 'runner', choices: ['bitscript', 'jest'], description: 'Select test runner'})
    .option('t', {alias: 'teardown', type: 'boolean', default: false, description: 'Tear down environment after run'})
    .help('h')
    .argv as any as IArgv

async function asyncMain() {
    process.env.RD_URL = DEFAULT_RD_URL
    process.env.RD_USER = DEFAULT_RD_USER
    process.env.RD_PASSWORD = DEFAULT_RD_PASSWORD

    const client = new Client({apiUrl: DEFAULT_RD_URL, username: DEFAULT_RD_USER, password: DEFAULT_RD_PASSWORD})

    const envBuilder = new EnvBuilder(client)

    await envBuilder.up()

    if (argv.runner == 'bitscript') {
        const testRepo = await TestRepo.CreateTestRepo('./tests', new RegExp(argv.filter))
        const testRunner = new BitScriptRunner(testRepo)
        await testRunner.run()
    } else {
        const ret = await spawn('/bin/sh', ['-c', `./node_modules/.bin/jest ${argv.jest}`], {stdio: 'inherit'})
        if (ret != 0)
            process.exitCode = 1
    }

    if (argv.teardown)
        await envBuilder.down()
}

asyncMain()