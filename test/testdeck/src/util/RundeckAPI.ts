import {WebResource, RequestPolicy, RequestPolicyFactory, RequestPolicyOptions, BaseRequestPolicy, HttpOperationResponse} from '@azure/ms-rest-js'
import {Rundeck} from 'ts-rundeck'
import {Status, ExecutionStatusGetResponse, RundeckJobExecutionRunOptionalParams} from 'ts-rundeck/dist/lib/models'
import {combineCookies} from 'ts-rundeck/dist/util'

import {sleep} from '../async/util'

export async function waitForRundeckReady(client: Rundeck, timeout = 500000) {
    await createWaitForRundeckReady(() => client, timeout)
}
export async function createWaitForRundeckReady(factory: ()=>Rundeck, timeout = 500000) {
    const start = Date.now()
    const unauthMax=10
    const sleepTime=2000
    let unauthCount=0
    console.info(`Waiting for server to accept requests...`)
    while (Date.now() - start < timeout) {
        try {
            const reqstart=Date.now()
            let resp = await factory().systemInfoGet()
            console.info(`Client connected: ${resp.system?.rundeckProperty?.version} (${Date.now() - reqstart}ms)`)
            return
        } catch  (e) {
            if (e.statusCode === 403) {
                unauthCount++
            }
            // if (e.statusCode) {
            //     console.debug(`Received status... ${e.statusCode}: ${e}`)
            // } else if(e.code){
            //     console.debug(`Waiting... ${e.code}: ${e}`)
            // } else {
            //     console.debug(`Waiting... ${Date.now() - start}`)
            // }
            if (unauthCount > unauthMax) {
                throw new Error(`Rundeck authentication failure: ${e}`)
            }
            await sleep(sleepTime)
        }
    }
    throw new Error('Timeout exceeded waiting for Rundeck to be ready.')
}

const STATUS_FINAL = [
    Status.Aborted,
    Status.Failed,
    Status.Succeeded,
    Status.Timedout
]

export async function waitForExecutionComplete(client: Rundeck, id: number) {
    let resp: ExecutionStatusGetResponse

    let curStatus = Status.Running
    while(true) {
        resp = await client.executionStatusGet(id.toString())
        curStatus = resp.status

        if (STATUS_FINAL.includes(curStatus))
            break
        else
            await sleep(1000)
    }

    return resp
}

export async function runJobAndWait(client: Rundeck, id: string, options?: RundeckJobExecutionRunOptionalParams) {
    const resp = await client.jobExecutionRun(id, options)

    return await waitForExecutionComplete(client, resp.id)
}

export function cookieEnrichPolicy(cookies: string[]): RequestPolicyFactory {
    return {
        create: (nextPolicy: RequestPolicy, options: RequestPolicyOptions) => {
            return new CookieEnrichPolicy(nextPolicy, options, cookies)
        }
    }
}

/** Enriches each request with a set of cookies */
export class CookieEnrichPolicy extends BaseRequestPolicy {
    constructor(nextPolicy: RequestPolicy, options: RequestPolicyOptions, readonly cookies: string[]) {
        super(nextPolicy, options)
    }

    async sendRequest(webResource: WebResource): Promise<HttpOperationResponse> {
        const reqCookies = webResource.headers.get('cookie')
        const combinedCookies = combineCookies(reqCookies, this.cookies)

        webResource.headers.set('cookie', combinedCookies.join(';'))

        return await this._nextPolicy.sendRequest(webResource)
    }
}
