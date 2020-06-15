import {ExecutionOutput, ExecutionOutputStore} from '../../src/stores/ExecutionOutput'
import {ExecutionLog} from '../../src/utilities/ExecutionLogConsumer'

import {observe, autorun} from 'mobx'
import {} from './TsRundeckMock'
import { PasswordCredentialProvider, passwordAuthPolicy, rundeckPasswordAuth } from 'ts-rundeck'
import {RundeckVcr, Cassette} from 'ts-rundeck/dist/util/RundeckVcr'
import { BaseCredentialProvider } from 'ts-rundeck/dist/baseCredProvider'
import { BtoA, AtoB } from '../utilities/Base64'

import {Failed} from '../data/ExecutionOutput'

import fetchMock from 'fetch-mock'

describe('ExecutionOutput Store', () => {
    it('Foos', async () => {
        const client = rundeckPasswordAuth('admin', 'admin', {baseUri: 'http://xubuntu:4440'})

        const vcr = new RundeckVcr()

        const cassette = new Cassette([/execution/, /job/], './public/fixtures/ExecFailedOutput.json')

        vcr.record(cassette)
    
        const consumer = new ExecutionLog('880', client)

        await consumer.init()

        await consumer.getJobWorkflow()

        await consumer.getOutput(200)

        await cassette.store()

    })
})