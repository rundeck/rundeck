import Vue from 'vue'
import {array ,object, withKnobs} from '@storybook/addon-knobs'

import { Rundeck, TokenCredentialProvider } from '@rundeck/client'
import {BrowserFetchHttpClient} from '@azure/ms-rest-js/es/lib/browserFetchHttpClient'

import '../../../stories/setup'

import {RootStore} from '../../../stores/RootStore'
import { ServerInfo, VersionInfo } from '../../../stores/System'

import UtilityBar from './UtilityBar.vue'
import RundeckInfo from './RundeckInfo.vue'
import RundeckInfoWidget from './RundeckInfoWidget.vue'

// @ts-ignore
window._rundeck.rundeckClient = new Rundeck(new TokenCredentialProvider(process.env.STORYBOOK_RUNDECK_TOKEN), {baseUri: 'http://xubuntu:4440', httpClient: new BrowserFetchHttpClient()})


export default {
    title: 'Widgets/Rundeck Info',
    decorators: [withKnobs]
}


export const infoDisplay = () => {
    const rootStore = new RootStore(window._rundeck.rundeckClient)
    const version = new VersionInfo()
    const server = new ServerInfo('xubuntu', 'f1dbb7ed-c575-4154-8d01-216a59d7cb5e')

    version.number = '3.4.0'
    version.name = 'Papadum'
    version.icon = 'book'
    version.color = 'aquamarine'

    return Vue.extend({
        template: `<RundeckInfo v-bind="$data"/>`,
        provide: {rootStore},
        components: {RundeckInfo},
        data: () => ({
            version,
            server,
        })
    })
}

export const infoWidget = () => {
    const rootStore = new RootStore(window._rundeck.rundeckClient)

    return Vue.extend({
        template: `<RundeckInfoWidget v-bind="$data"/>`,
        provide: {rootStore},
        components: {RundeckInfoWidget}
    })
}