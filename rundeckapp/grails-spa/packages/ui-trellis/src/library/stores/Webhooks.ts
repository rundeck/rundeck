import {RootStore} from './RootStore'
import { HttpOperationResponse } from '@azure/ms-rest-js/es/lib/httpOperationResponse'
import { v4 as uuidv4 } from 'uuid'

import {RundeckClient} from '@rundeck/client'

import {ServiceType, Plugin} from './Plugins'
import {reactive} from "vue";

export const webhookui = reactive({
    activeTab: 0,
    setActiveTab(idx:number) {
        this.activeTab = idx
    }
})

export class WebhookStore {
    webhooks: Webhook[] = []
    webhooksByUuid = new Map<string, Webhook>()

    loaded = new Map<string, boolean>()

    constructor(readonly root: RootStore, readonly client: RundeckClient) {
    }

    async load(project: string): Promise<void> {
        if (this.loaded.get(project))
            return

        await this.refresh(project)

        this.loaded.set(project, true)
    }

    async refresh(project: string): Promise<void> {
        const [_, resp] = await Promise.all([
            this.root.plugins.load(ServiceType.WebhookEvent),
            this.client.apiRequest({
                method: 'GET',
                pathTemplate: 'api/40/project/{project}/webhooks',
                pathParameters: {
                    project: project
                }
            })])

        resp.parsedBody.forEach((json: any) => {
            return this.addFromApi(json)
        })
    }

    remove(webhook: Webhook) {
        const stored = this.webhooksByUuid.get(webhook.uuid)
        if (stored) {
            this.webhooks.splice(this.webhooks.indexOf(stored), 1)
            this.webhooksByUuid.delete(webhook.uuid)
        }
    }

    add(webhook: Webhook) {
        const stored = this.webhooksByUuid.get(webhook.uuid)
        if (stored) {
            const index = this.webhooks.indexOf(stored)
            this.webhooks.splice(index, 1, webhook)
        } else {
            this.webhooks.push(webhook)
        }
        this.webhooksByUuid.set(webhook.uuid, webhook)
    }

    clone(webhook: Webhook) {
        const clone = this.newFromApi(webhook.toApi())
        clone.new = false
        return clone
    }

    webhooksForProject(project: string): Webhook[] {
        return this.webhooks.filter( wh => wh.project == project) || []
    }

    addFromApi(json: any) {
        const webhook = Webhook.FromApi(this, json)
        webhook.eventPlugin = this.root.plugins.getServicePlugins(ServiceType.WebhookEvent).find(p => p.artifactName == webhook.eventPluginName)
        webhook.new = false
        this.add(webhook)
    }

    newFromApi(json: any) {
        const webhook = Webhook.FromApi(this, json)
        webhook.eventPlugin = this.root.plugins.getServicePlugins(ServiceType.WebhookEvent).find(p => p.artifactName == webhook.eventPluginName)
        return webhook
    }

    async save(webhook: Webhook): Promise<HttpOperationResponse> {
        const resp = await this.client.apiRequest({
            method: 'POST',
            pathTemplate: 'api/40/project/{project}/webhook/{webhookId}',
            pathParameters: {
                project: webhook.project,
                webhookId: webhook.id.toString()
            },
            body: webhook.toApi()
        })

        if (resp.status == 200) {
            webhook.new = false
            this.add(webhook)
        }

        return resp
    }

    async create(webhook: Webhook): Promise<HttpOperationResponse> {
        const resp = await this.client.apiRequest({
            method: 'POST',
            pathTemplate: 'api/40/project/{project}/webhook',
            pathParameters: {
                project: webhook.project,
            },
            body: webhook.toApi()
        })

        return resp
    }

    async delete(webhook: Webhook): Promise<HttpOperationResponse> {
        const resp = await this.client.apiRequest({
            method: 'DELETE',
            pathTemplate: 'api/40/project/{project}/webhook/{webhookId}',
            pathParameters: {
                project: webhook.project,
                webhookId: webhook.id.toString()
            }
        })
        if (resp.status == 200) {
            this.remove(webhook)
        }

        return resp
    }
}

export class Webhook {
     uuid: string = uuidv4()
     id!: string
     authToken!: string
     enabled!: boolean
     name!: string
     creator!: string
     config: any = {}
     project!: string
     roles!: string
     user!: string
     useAuth!: boolean
     regenAuth!: boolean
     authString!: string
     eventPluginName!: string
     eventPlugin?: Plugin

     new = true

    constructor(readonly store: WebhookStore) {}

    static FromApi(store: WebhookStore, json: any) {
        const webhook = new Webhook(store)
        webhook.fromApi(json)
        return webhook
    }

    fromApi(json: any) {
        this.uuid = json.uuid || this.uuid
        this.id = json.id
        this.authToken = json.authToken
        this.enabled = json.enabled
        this.name = json.name
        this.creator = json.creator
        this.config = json.config
        this.project = json.project
        this.roles = json.roles
        this.user = json.user
        this.useAuth = json.useAuth
        this.regenAuth = json.regenAuth
        this.authString = json.authString
        this.eventPluginName = json.eventPlugin
    }

    toApi() {
        const config = this.config.__ob__ ? this.config.__ob__.value : this.config

        return {
            uuid: this.uuid,
            id: this.id,
            authToken: this.authToken,
            enabled: this.enabled,
            name: this.name,
            creator: this.creator,
            config: config,
            project: this.project,
            roles: this.roles,
            user: this.user,
            useAuth: this.useAuth,
            regenAuth: this.regenAuth,
            authString: this.authString,
            eventPlugin: this.eventPlugin?.name
        }
    }
}