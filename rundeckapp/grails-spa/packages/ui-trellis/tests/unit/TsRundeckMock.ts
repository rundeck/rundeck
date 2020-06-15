import Axios, { AxiosRequestConfig, AxiosInstance } from 'axios'
import Mock from 'axios-mock-adapter'
import {RundeckClient, TokenCredentialProvider} from 'ts-rundeck'
import {RundeckOptions} from 'ts-rundeck/dist/lib/models'

import { isArray } from 'util'
import { AtoB } from '../utilities/Base64'
import { stringify } from 'querystring'

interface ResponseRecord extends Response {
    headers: Headers
    ok: boolean
    redirected: boolean;
    status: number;
    statusText: string;
    type: ResponseType;
    url: string;
}

class RundeckRecorder {
    realFetch: typeof global.fetch

    constructor() {
        this.realFetch = global.fetch
        global.fetch = async (input: RequestInfo, init?: RequestInit): Promise<Response> => {
            const start = Date.now()
            const resp = await this.realFetch(input, init)
            console.log(Date.now() - start)
            const rec = resp.clone()
            console.log(rec)
            return resp
        }
    }
}
