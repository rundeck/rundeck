import {CreateTestContext} from '@rundeck/testdeck/test/api'

import {readFile} from '@rundeck/testdeck/async/fs'
import '@rundeck/testdeck/test/rundeck'
import {runJobAndWait} from '@rundeck/testdeck/util/RundeckAPI'

/** Fetch context which will be initialized before the tests run */
let context = CreateTestContext({
    projects: ['TestJobs']
})

describe('Jobs', () => {
    it('Runs workflow steps', async () => {
        const {cluster} = context

        /** Load files onto cluster */
        const SCRIPT_FILE1='job-run-steps-test-script1.txt'

        let buffer = await readFile(`./lib/job/${SCRIPT_FILE1}`)
        await cluster.writeRundeckFile(SCRIPT_FILE1, buffer)

        /** Run job and collect log output */
        const runResp = await runJobAndWait(
            cluster.client,
            '9b43e4ab-7ff2-4159-9fc7-7437901914f7',
            {request: {options:{opt2: 'a'}}}
        )
        const log = await cluster.client.executionOutputGet(runResp.id.toString())

        /** Compare log output to expected */
        const expectedOut = [
            'hello there',
            'option opt1: testvalue',
            'option opt1: testvalue',
            expect.any(String),
            'option opt2: a',
            'this is script 2, opt1 is testvalue',
            'hello there',
            'this is script 1, opt1 is testvalue',
        ]
        let logTxt = log.entries.map(e => e.log)
        expect(logTxt).toEqual(expectedOut)
    })
})
