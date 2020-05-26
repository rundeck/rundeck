import Axios, { AxiosRequestConfig, AxiosInstance } from 'axios'
import Mock from 'axios-mock-adapter'
import {RundeckClient, TokenCredentialProvider} from 'ts-rundeck'
import {RundeckOptions} from 'ts-rundeck/dist/lib/models'

import {
    ServiceClientCredentials,
    WebResource,
    HttpOperationResponse,
    HttpHeaders,
    BaseRequestPolicy,
    RequestPolicy,
    RequestPolicyFactory,
    RequestPolicyOptions} from '@azure/ms-rest-js'
import { isArray } from 'util'
import { AtoB } from '../utilities/Base64'
import { stringify } from 'querystring'

export class MockClient {
    constructor(readonly axios: AxiosInstance) {}

    async sendRequest(httpRequest: WebResource): Promise<HttpOperationResponse> {
        const cancelToken = Axios.CancelToken.source().token
        const config: AxiosRequestConfig = {
            method: httpRequest.method as any,
            url: httpRequest.url,
            // headers: rawHeaders,
            // data: axiosBody,
            transformResponse: undefined,
            validateStatus: () => true,
            // Workaround for https://github.com/axios/axios/issues/1362
            maxContentLength: Infinity,
            responseType: httpRequest.streamResponseBody ? "stream" : "text",
            cancelToken,
            timeout: httpRequest.timeout
          };
          const res = await this.axios(config)

        return {
            request: httpRequest,
            status: res.status,
            headers: new HttpHeaders(res.headers),
            readableStreamBody: undefined,
            bodyAsText: httpRequest.streamResponseBody ? undefined : res.data as string
          };
    }
}

export interface MockResp {
    url: string
    method: 'GET' | 'POST' | 'PUT'
    status: number
    body: string | null | undefined
    reqHeaders?: any
    respHeaders?: any
}

export function GetMockClient(responses: MockResp[]): RundeckClient {
    const axios = Axios.create()
    const mock = new Mock(axios)

    const responseMap: Map<string, MockResp[]> = new Map()

    for (let resp of responses) {
        console.log(resp)
        const key = `${resp.method} ${resp.url}`

        let entries: MockResp[]

        if (!responseMap.has(key)) {
            entries = []
            responseMap.set(key, entries)
        } else {
            entries = responseMap.get(key)!
        }

        entries.push(resp)
    }

    for (let [k, v] of responseMap.entries()) {

        const [method, url] = k.split(' ')

        console.log(k)
        const parsedUrl = new URL(url)

        const mockUrl = `http://localhost${parsedUrl.pathname}${parsedUrl.search}`

        console.log(mockUrl)

        const respFunc = (config: any): any[] => {
            console.log(config)
            const resp = v.shift()!
            const reply = [
                resp.status,
                resp.body,
                resp.respHeaders
            ]
            return reply
        }
        switch(method) {
            case 'GET':
                mock.onGet(mockUrl).reply(respFunc)
            case 'POST':
                mock.onPost(mockUrl).reply(respFunc)
            case 'PUT':
                mock.onPut(mockUrl).reply(respFunc)
        }

        // mock.onAny(/.*/).reply( (config) => {
        //     console.log(config)
        //     return [200]
        // })
    }

    const httpClient = new MockClient(axios)
    const client = new RundeckClient(new TokenCredentialProvider('foo'), {baseUri: '', httpClient})
    return client
}

export function recordPolicy(func: (resp: MockResp) => void): RequestPolicyFactory {
    return {
        create: (nextPolicy: RequestPolicy, options: RequestPolicyOptions) => {
            return new ResponseRecordPolicy(nextPolicy, options, func)
        }
    }
}

export class ResponseRecordPolicy extends BaseRequestPolicy {
    constructor(nextPolicy: RequestPolicy, options: RequestPolicyOptions, readonly func: (resp: MockResp) => void) {
        super(nextPolicy, options)
    }
    async sendRequest(webResource: WebResource) {

        const resp = await this._nextPolicy.sendRequest(webResource)

        this.func({
            url: webResource.url,
            method: webResource.method as 'GET' | 'POST' | 'PUT',
            reqHeaders: webResource.headers,
            status: resp.status,
            body: resp.bodyAsText,
            respHeaders: resp.headers
        })

        return resp
    }
}

export function rundeckRecorder(cred: ServiceClientCredentials, opts: RundeckOptions, func: (resp: MockResp) => void): RundeckClient {
    if (!opts.baseUri)
        throw new Error('Must supplie opts.baseUri')

    const recPolicy = recordPolicy(func)

    let policies = [recPolicy]

    if (Array.isArray(opts.requestPolicyFactories))
        policies = [...opts.requestPolicyFactories, ...policies]

    opts.requestPolicyFactories = (def) => {
        return [...policies, ...def]
    }

    return new RundeckClient(
        cred,
        opts
    )
}