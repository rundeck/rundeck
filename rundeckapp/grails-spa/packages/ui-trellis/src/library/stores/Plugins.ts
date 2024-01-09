import {RootStore} from './RootStore'
import {RundeckClient} from '@rundeck/client'

import { Serial } from '../utilities/Async'

export class PluginStore {
    plugins: Plugin[] = []

    pluginsByService: { [key: string]: Plugin[]} = {}
    pluginsById: { [key: string]: Plugin[]} = {}

    constructor(readonly root: RootStore, readonly client: RundeckClient) {
    }

    @Serial
    async load(service?: string): Promise<void> {
        const plugins = await this.client.apiRequest({
            pathTemplate: 'api/40/plugin/list',
            queryParameters: {
                service
            },
            method: 'GET'
        })

        plugins.parsedBody.forEach((p: any) => {
            if (this.pluginsById[p.name])
                return
            else
                this.plugins.push(p)
        });
        this._refreshPluginGroups()
        return void(0)
    }

    _refreshPluginGroups() {
        this.pluginsByService = this.plugins.reduce((r, p) => {
            const svcKey = `${p.service}`
            r[svcKey] = r[svcKey] || []
            r[svcKey].push(p)
            return r
        }, Object.create(null))
        this.pluginsById = this.plugins.reduce((r, p) => {
            const svcKey = p.name
            r[svcKey] = r[svcKey] || []
            r[svcKey].push(p)
            return r
        }, Object.create(null))
    }

    getServicePlugins(service: string): Plugin[] {
        return this.pluginsByService[service]?.sort((a, b) => a.title.localeCompare(b.title)) || []
    }
}


export interface Plugin {
    id: string
    name: string
    artifactName: string
    title: string
    description: string
    author: string
    builtin: boolean
    pluginVersion: string
    service: string
    iconUrl?: string
    providerMetadata?: {
        glyphicon?: string
        faicon?: string
        fabicon?: string
    }
}

export enum ServiceType {
    WebhookEvent = 'WebhookEvent'
}