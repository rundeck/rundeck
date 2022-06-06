import Vue from 'vue'

import { Rundeck, TokenCredentialProvider } from '@rundeck/client'
import {BrowserFetchHttpClient} from '@azure/ms-rest-js/es/lib/browserFetchHttpClient'

import '../../../stories/setup'

import {RootStore} from '../../../stores/RootStore'

import ProjectSelectButton from './ProjectSelectButton.vue'
import ProjectSelect from './ProjectSelect.vue'

// @ts-ignore
window._rundeck.rundeckClient = new Rundeck(new TokenCredentialProvider(process.env.STORYBOOK_RUNDECK_TOKEN), {baseUri: process.env.STORYBOOK_RUNDECK_URL, httpClient: new BrowserFetchHttpClient()})


export default {
    title: 'Widgets/Project Select'
}


export const projectPicker = () => {
    const rootStore = new RootStore(window._rundeck.rundeckClient)
    
    return Vue.extend({
        template: `<ProjectSelect v-bind="$data"/>`,
        provide: {rootStore},
        components: {ProjectSelect},
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

export const pickerButton = () => {
    const rootStore = new RootStore(window._rundeck.rundeckClient)
    
    return Vue.extend({
        template: `<ProjectSelectButton v-bind="$data"/>`,
        provide: {rootStore},
        components: {ProjectSelectButton},
        data: () => ({
            projectLabel: 'Test'
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

export const pickerButtonNoLabel = () => {
    const rootStore = new RootStore(window._rundeck.rundeckClient)
    
    return Vue.extend({
        template: `<ProjectSelectButton v-bind="$data"/>`,
        provide: {rootStore},
        components: {ProjectSelectButton},
        data: () => ({
            projectLabel: ''
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