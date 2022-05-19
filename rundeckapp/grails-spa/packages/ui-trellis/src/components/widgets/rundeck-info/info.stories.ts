import Vue from 'vue'

import {RootStore} from '../../../stores/RootStore'
import { ServerInfo, VersionInfo } from '../../../stores/System'
import { Release } from '../../../stores/Releases'

import RundeckInfo from './RundeckInfo.vue'
import RundeckInfoWidget from './RundeckInfoWidget.vue'

export default {
    title: 'Widgets/Rundeck Info'
}

export const infoDisplay = () => {
    console.log(window._rundeck.rundeckClient)
    const rootStore = new RootStore(window._rundeck.rundeckClient)
    const version = new VersionInfo()
    const server = new ServerInfo('xubuntu', 'f1dbb7ed-c575-4154-8d01-216a59d7cb5e')

    version.number = '3.4.0'
    version.name = 'Papadum'
    version.icon = 'book'
    version.color = 'aquamarine'
    version.edition = 'Community'

    const latest = new Release()

    latest.full = '3.4.0-20210301'
    latest.number = '3.4.0'
    latest.name = 'Papadum'
    latest.icon = 'book'
    latest.color = 'aquamarine'
    latest.edition = 'Community'
    latest.date = new Date(Date.parse('2021-01-01'))

    return Vue.extend({
        template: `<RundeckInfo v-bind="$data"/>`,
        provide: {rootStore},
        components: {RundeckInfo},
        data: () => ({
            version,
            latest,
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