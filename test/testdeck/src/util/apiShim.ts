import CP from 'child_process'
import FS from 'fs'

import {envOpts} from '../test/rundeck'
import { CreateTestContext } from '../test/api'

const skipTests = [
    'test-execution-cleaner-job.sh', // Does not handle `null` for execution server UUID in a cluster
    'test-execution-output-plain-lastlines.sh',
    'test-execution-output-plain.sh',
    'test-execution-output-utf8.sh',
    'test-execution-state.sh', // Reads framework.properties

    /** Misc */
    'test-history.sh',
    'test-metrics.sh', // .meters length 7 instead of 8 ?
    'test-v23-project-sources-xml.sh',
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
