import {RootStore} from './RootStore'
import {RundeckClient} from '@rundeck/client'
import { action, computed, flow, observable } from 'mobx'

import { Serial } from '../utilities/Async'

export class PluginStore {
    @observable plugins: Array<Plugin> = []

    constructor(readonly root: RootStore, readonly client: RundeckClient) {}

    @action
    async load(): Promise<void> {
        const plugins = await this.client.sendRequest({
            pathTemplate: `/plugin/list`,
            baseUrl: 'http://localhost:8080/api/33',
            method: 'GET'
        })

        this.plugins = plugins.parsedBody as Array<Plugin>
        console.log(plugins)
    }
}


interface Plugin {
    name: string
    description: string
    iconUrl?: string
    artifactName: string
}