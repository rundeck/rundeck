import Vue from 'vue'
import { array, object, withKnobs } from '@storybook/addon-knobs'

import { Rundeck, TokenCredentialProvider } from '@rundeck/client'
import { BrowserFetchHttpClient } from '@azure/ms-rest-js/es/lib/browserFetchHttpClient'

import '../../stories/setup'

import { RootStore } from '../../stores/RootStore'

import TRow from './Row.vue'
import THeader from './Header.vue'
import RdTable from './RdTable.vue'

// @ts-ignore
window._rundeck.rundeckClient = new Rundeck(new TokenCredentialProvider(process.env.STORYBOOK_RUNDECK_TOKEN), { baseUri: process.env.STORYBOOK_RUNDECK_URL, httpClient: new BrowserFetchHttpClient() })


export default {
    title: 'RD Table',
    decorators: [withKnobs]
}

export const tableRow = () => {
    const rootStore = new RootStore(window._rundeck.rundeckClient)
    return Vue.extend({
        template: `<TRow v-bind="$data"/>`,
        provide: { rootStore },
        components: { TRow },
        data: () => ({
            projectLabel: '',
        }),
        mounted() {
        }
    })
}

export const tableHeader = () => {
    const rootStore = new RootStore(window._rundeck.rundeckClient)

    return Vue.extend({
        template: `<THeader v-bind="$data"/>`,
        provide: { rootStore },
        components: { THeader },
        data: () => ({
            projectLabel: '',
        }),
        mounted() {
        }
    })
}

export const rdTable = () => {
    const rootStore = new RootStore(window._rundeck.rundeckClient)

    return Vue.extend({
        template: `<RdTable v-bind="$data"/>`,
        provide: { rootStore },
        components: { RdTable },
        data: () => ({
            projectLabel: '',
        }),
        mounted() {
        }
    })
}
