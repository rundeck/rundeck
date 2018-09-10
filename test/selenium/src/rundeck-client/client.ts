import Axios, { AxiosInstance, AxiosRequestConfig, AxiosResponse } from 'axios'

import * as Dto from './dto'

interface IClientOpts {
    apiUrl: string
    username?: string
    password?: string
    token?: string
}

export class Client {
    c: AxiosInstance

    private loginProm?: Promise<void>

    constructor(readonly opts: IClientOpts) {
        this.c = Axios.create({
            headers: {
                'Content-Type': 'application/json',
            },
            maxRedirects: 0,
        })
    }

    login() {
        if (! this.loginProm)
            this.loginProm = this._doLogin()
        return this.loginProm
    }

    async listUsers(): Promise<AxiosResponse<Dto.IUser[]>> {
        const path = `${this.opts.apiUrl}/api/21/user/list`
        return this.get<Dto.IUser[]>(path)
    }

    async systemInfo(): Promise<AxiosResponse<Dto.ISystemInfoResponse>> {
        const path = `${this.opts.apiUrl}/api/14/system/info`
        return this.get<Dto.ISystemInfoResponse>(path)
    }

    async get<T>(url: string, config?: AxiosRequestConfig) {
        await this.login()
        return await this.c.get<T>(url, config)
    }

    private async _doLogin(): Promise<void> {
        try {
            const {apiUrl, username, password} = this.opts
            const path = `${apiUrl}/j_security_check?j_username=${username}&j_password=${password}`
            const resp = await this.c.post(path, null, {validateStatus: s => s >= 300 && s <= 400})
            this.c.defaults.headers.Cookie = resp.headers['set-cookie'][0]
        } catch (e) {
            this.loginProm = undefined
            throw e
        }
    }
}