import Vue from 'vue'

import { Rundeck, TokenCredentialProvider } from '@rundeck/client'
import { BrowserFetchHttpClient } from '@azure/ms-rest-js/es/lib/browserFetchHttpClient'

import '../../stories/setup'

import { RootStore } from '../../stores/RootStore'

import { ServerInfo, VersionInfo } from '../../stores/System'

import Firstrun from './FirstRun.vue'

// @ts-ignore
window._rundeck.rundeckClient = new Rundeck(new TokenCredentialProvider(process.env.STORYBOOK_RUNDECK_TOKEN), { baseUri: process.env.STORYBOOK_RUNDECK_URL, httpClient: new BrowserFetchHttpClient() })


export default {
    title: 'First Run'
}

export const firstRun = () => {
    const rootStore = new RootStore(window._rundeck.rundeckClient)
    const version = new VersionInfo()
    const server = new ServerInfo('xubuntu', 'f1dbb7ed-c575-4154-8d01-216a59d7cb5e')

    version.number = '3.4.0'
    version.name = 'Papadum'
    version.icon = 'book'
    version.color = 'aquamarine'

    return Vue.extend({
        template: `<Firstrun v-bind="$data"/>`,
        provide: { rootStore },
        components: { Firstrun },
        data: () => ({
            projectLabel: '',
            version,
            server
        }),
        mounted() {
            document.documentElement.dataset.colorTheme = 'dark'
            const el = this.$el as any
            el.parentNode.style.height = '100vh'
            el.parentNode.style.overflow = 'hidden'
            el.parentNode.style.position = 'relative'
            el.parentNode.style.padding = '20px'
            document.body.style.overflow = 'hidden'
        }
    })
}
