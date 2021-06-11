import Vue from 'vue'
import {array ,object, withKnobs} from '@storybook/addon-knobs'

import { Rundeck, TokenCredentialProvider } from '@rundeck/client'
import {BrowserFetchHttpClient} from '@azure/ms-rest-js/es/lib/browserFetchHttpClient'

import '../../../stories/setup'

import {RootStore} from '../../../stores/RootStore'

import PluginSelect from './PluginSelect.vue'

// @ts-ignore
window._rundeck.rundeckClient = new Rundeck(new TokenCredentialProvider(process.env.STORYBOOK_RUNDECK_TOKEN), {baseUri: process.env.STORYBOOK_RUNDECK_URL, httpClient: new BrowserFetchHttpClient()})


export default {
    title: 'Widgets/Webhook Select',
    decorators: [withKnobs]
}


export const pluginPicker = () => {
    const rootStore = new RootStore(window._rundeck.rundeckClient)
    
    return Vue.extend({
        template: `<PluginSelect v-bind="$data"/>`,
        provide: {rootStore},
        components: {PluginSelect},
        data: () => ({

        }),
        mounted() {
            const el = this.$el as any
            el.parentNode.style.height = '100vh'
            el.parentNode.style.overflow = 'hidden'
            el.parentNode.style.position = 'relative'
            el.parentNode.style.padding = '20px'
            document.body.style.overflow = 'hidden'
        }
    })
}
