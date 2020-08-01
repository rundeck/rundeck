console.log(global.fetch)

import Util from 'util'

import {ExecutionOutput, ExecutionOutputStore} from '../../src/stores/ExecutionOutput'
import {ExecutionLog} from '../../src/utilities/ExecutionLogConsumer'

import {observe, autorun, intercept} from 'mobx'
import { PasswordCredentialProvider, passwordAuthPolicy, rundeckPasswordAuth, RundeckClient, TokenCredentialProvider } from '@rundeck/client'
import {RundeckVcr, Cassette} from '@rundeck/client/dist/util/RundeckVcr'
import { BtoA, AtoB } from '../utilities/Base64'
import { RootStore } from '../../src/stores/RootStore'
import fetchMock from 'fetch-mock'

jest.setTimeout(60000)

describe('ExecutionOutput Store', () => {
    it('true', () => {})
    // it('Loads Output', async () => {
    //     const client = new RundeckClient(new TokenCredentialProvider('foo'), {baseUri: '/'})

    //     const vcr = new RundeckVcr(fetchMock)
    //     const cassette = await Cassette.Load('./tests/data/fixtures/ExecStateBranching.json')
    //     vcr.play(cassette, fetchMock)

    //     const rootStore = new RootStore(client)

    //     console.log(await client.executionStateGet('1073'))

    //     while(true)
    //         console.log(JSON.stringify(await client.executionStateGet('1073'), null, 2))

    // })
    // it('Saves', async () => {
    //     jest.setTimeout(60000)
    //     const client = rundeckPasswordAuth('admin', 'admin', {baseUri: 'http://xubuntu:4440'})

    //     const vcr = new RundeckVcr(fetchMock)

    //     const run = await client.jobExecutionRun('825b3ed7-3d40-418f-bfb4-313ff4d50577')

    //     const cassette = new Cassette([/execution/, /job/], './tests/data/fixtures/ExecStateBranching.json')

    //     vcr.record(cassette)
    
    //     let finished = false
    //     while(!finished) {
    //         const state = await client.executionStateGet(run.id!.toString())
    //         finished = (state.executionState == 'SUCCEEDED')
    //         await new Promise((res) => {setTimeout(res, 1000)})
    //     }
    //     await cassette.store()
    // })
})