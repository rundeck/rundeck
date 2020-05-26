import {ExecutionOutput, ExecutionOutputStore} from '../../src/stores/ExecutionOutput'
import {ExecutionLog} from '../../src/utilities/ExecutionLogConsumer'

import {observe, autorun} from 'mobx'
import { GetMockClient, MockResp, rundeckRecorder } from './TsRundeckMock'
import { PasswordCredentialProvider, passwordAuthPolicy } from 'ts-rundeck'
import { BaseCredentialProvider } from 'ts-rundeck/dist/baseCredProvider'
import { BtoA, AtoB } from '../utilities/Base64'

import {Failed} from '../data/ExecutionOutput'

describe('ExecutionOutput Store', () => {
    it('Foos', async () => {
        // const store = new ExecutionOutputStore()

        // const client = GetMockClient(JSON.parse(AtoB(Failed)))

        // const log = new ExecutionLog('880', client)

        // console.log(await log.getOutput(5000))

        // const Foo = class {
        //     constructor() {}

        //     foo = autorun(() => {
        //         console.log(store.executionOutputsById.get('foo'))
        //     })
        // }

        // const foo = new Foo()

        // store.executionOutputsById.set('foo', 1)

        const pass = passwordAuthPolicy('http://xubuntu:4440', 'admin', 'admin')

        const responses: MockResp[] = []

        const rec = rundeckRecorder(new BaseCredentialProvider(), {baseUri: 'http://xubuntu:4440', requestPolicyFactories: [pass]},
            (resp) => {
                responses.push(resp)
            }
        )
        const consumer = new ExecutionLog('880', rec)

        await consumer.init()

        await consumer.getJobWorkflow()

        await consumer.getOutput(200)

        responses.forEach(element => {
            console.log(element.url)
        });

        console.log(BtoA(JSON.stringify(responses)))
    })
})