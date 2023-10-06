import CP from 'child_process'
import FS from 'fs'

import {envOpts} from '../test/rundeck'
import { CreateTestContext } from '../test/api'

const skipTests = [
    'test-job-run-steps.sh', // Requires file on Rundeck server(s)
    'test-job-run-webhook.sh', // Requires NC running
    'test-job-run-without-deadlock.sh', // Requires inspecting log output

    'test-execution-cleaner-job.sh', // Does not handle `null` for execution server UUID in a cluster
    'test-execution-output-plain-lastlines.sh',
    'test-execution-output-plain.sh',
    'test-execution-output-utf8.sh',
    //'test-execution-state.sh', // Reads framework.properties

    '^test-scm',

    /** Misc */
    'test-history.sh',
    'test-metrics.sh', // .meters length 7 instead of 8 ?
    'test-require-version.sh',
    'test-resource.sh',
    'test-resources.sh',
    'test-run-script-interpreter.sh',
    'test-run-script.sh',
    'test-v23-project-source-resources.sh',
    'test-v23-project-sources-json.sh',
    'test-v23-project-sources-xml.sh',
    'test-workflow-errorhandler.sh',
]

export function ShimApiTests(pattern: RegExp) {
    CreateTestContext({projects: ['test']})

    beforeAll(async () => {
        const out = CP.execSync(`RDECK_URL=${envOpts.TESTDECK_RUNDECK_URL} SHELL=/bin/bash bash ./rundecklogin.sh - admin admin`, {cwd: '../api'})
    })

    let tests = FS.readdirSync('../api')

    tests = tests.filter(t => pattern.test(t) && t.endsWith('.sh'))

    for (let t of tests) {
        if (skipTests.some(s => new RegExp(s).test(t))) {
            console.log(`Skipping ${t}`)
            it.skip(t, () => {})
            continue
        }
 
        it(t, () => {
            try {
                const out = CP.execSync(`RDECK_URL=${envOpts.TESTDECK_RUNDECK_URL} SHELL=/bin/bash bash ./${t} -`, {cwd: '../api'})
            } catch (e:any) {
                const ex = e as Error
                ex.message = `${e.stdout.toString()}\n${ex.message}`
                throw e
            }
        })
    }
}
