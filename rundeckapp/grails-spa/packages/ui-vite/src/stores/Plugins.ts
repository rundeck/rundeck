import {RootStore} from './RootStore'
import {RundeckClient} from '@rundeck/client'
import { observable, IObservableArray } from 'mobx'
import {ObservableGroupMap, actionAsync, task, asyncAction} from 'mobx-utils'

import { Serial } from '../utilities/Async'

export class PluginStore {
    @observable plugins: IObservableArray<Plugin> = observable.array()

    @observable.shallow pluginsByService: ObservableGroupMap<string, Plugin>
    @observable.shallow pluginsById: ObservableGroupMap<string, Plugin>

    constructor(readonly root: RootStore, readonly client: RundeckClient) {
        this.pluginsByService = new ObservableGroupMap(this.plugins, (p) => `${p.service}`)
        this.pluginsById = new ObservableGroupMap(this.plugins, (p) => p.name)
    }

    @Serial
    @actionAsync
    async load(service?: string): Promise<void> {
        const plugins = await task(this.client.apiRequest({
            pathTemplate: 'api/40/plugin/list',
            queryParameters: {
                service
            },
            method: 'GET'
        }))

        plugins.parsedBody.forEach((p: any) => {
            if (this.pluginsById.has(p.name))
                return
            else
                this.plugins.push(p)
        });
        return void(0)
    }

    getServicePlugins(service: string) {
        return this.pluginsByService.get(service)?.sort((a, b) => a.title.localeCompare(b.title)) || []
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