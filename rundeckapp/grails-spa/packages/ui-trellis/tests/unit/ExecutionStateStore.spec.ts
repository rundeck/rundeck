console.log(global.fetch)

import Util from 'util'

import {observe, autorun, intercept} from 'mobx'
import { PasswordCredentialProvider, passwordAuthPolicy, rundeckPasswordAuth, RundeckClient, TokenCredentialProvider } from '@rundeck/client'
import {RundeckVcr, Cassette} from '@rundeck/client/dist/util/RundeckVcr'
import { RootStore } from '../../src/stores/RootStore'
import fetchMock from 'fetch-mock'

jest.setTimeout(60000)

describe('ExecutionOutput Store', () => {
    it('Loads Output', async () => {
        const client = new RundeckClient(new TokenCredentialProvider('foo'), {baseUri: '/'})

        const vcr = new RundeckVcr(fetchMock)
        const cassette = await Cassette.Load('./tests/data/fixtures/ExecStateBranching.json')
        vcr.play(cassette, fetchMock)

        const rootStore = new RootStore(client)

        const state = await rootStore.executionStateStore.fetch('1073')

        let last: any

        autorun( () => {
            console.log(state.state)
            const node = state.nodes.get('nginx-1')

            console.log(node === last)

            last = node

            if (node) {
                console.log(node.steps.map(s => s.stepctx))
            }
        })

        autorun( () => {
            if (state.workflow) {
                const {workflow} = state
                console.log({
                    completed: workflow.completed,
                    steps: workflow.stepCount
                })

                let stepCount = (state.workflow.steps.filter(s => s.state == 'SUCCEEDED') || []).length
                let totalStepCount = (Array.from(state.steps.values()).filter(s => s.state == 'SUCCEEDED') || []).length
                console.log(`Completed steps: ${stepCount}`)
                console.log(`All completed steps: ${totalStepCount}`)
            }
        })

        let finished = false
        while(!finished) {
            await new Promise(res => setTimeout(res, 50))
            finished = (await rootStore.executionStateStore.fetch('1073')).state == 'SUCCEEDED'
        }
    })

})