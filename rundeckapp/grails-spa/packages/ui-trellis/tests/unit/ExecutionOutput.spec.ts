import {ExecutionOutput, ExecutionOutputStore} from '../../src/stores/ExecutionOutput'
import {ExecutionLog} from '../../src/utilities/ExecutionLogConsumer'

import {observe, autorun} from 'mobx'
import { PasswordCredentialProvider, passwordAuthPolicy, rundeckPasswordAuth } from 'ts-rundeck'
import {RundeckVcr, Cassette} from 'ts-rundeck/dist/util/RundeckVcr'
import { BtoA, AtoB } from '../utilities/Base64'

describe('ExecutionOutput Store', () => {
    it('Foos', async () => {
        jest.setTimeout(60000)
        const client = rundeckPasswordAuth('admin', 'admin', {baseUri: 'http://xubuntu:4440'})

        const vcr = new RundeckVcr()

        const run = await client.jobExecutionRun('3cdda630-28a2-42b2-a873-ec932d84ed27')

        const cassette = new Cassette([/execution/, /job/], './public/fixtures/ExecRunningOutput.json')

        vcr.record(cassette)
    
        const consumer = new ExecutionLog(run.id!.toString(), client)

        await consumer.init()

        await consumer.getJobWorkflow()

        let finished: boolean = false
        while (!finished) {
            let resp = await consumer.getOutput(200)
            finished = consumer.completed
        }

        await cassette.store()

    })
})