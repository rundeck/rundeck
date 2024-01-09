import type {Meta, StoryFn} from "@storybook/vue3"

import { Rundeck, TokenCredentialProvider } from '@rundeck/client'
import {BrowserFetchHttpClient} from '@azure/ms-rest-js/es/lib/browserFetchHttpClient'

import '../../../stories/setup'

import {RootStore} from '../../../stores/RootStore'

import ProjectSelect from './ProjectSelect.vue'

// @ts-ignore
window._rundeck.rundeckClient = new Rundeck(new TokenCredentialProvider(process.env.STORYBOOK_RUNDECK_TOKEN), {baseUri: process.env.STORYBOOK_RUNDECK_URL, httpClient: new BrowserFetchHttpClient()})


export default {
    title: 'Widgets/Project Select',
    component: ProjectSelect
} as Meta<typeof ProjectSelect>


export const projectPicker: StoryFn<typeof ProjectSelect> = () => {
    const rootStore = new RootStore(window._rundeck.rundeckClient)
    
    return {
        template: `<ProjectSelect />`,
        provide: {rootStore},
        components: {ProjectSelect},
        mounted() {
            const el = this.$el as any
            el.parentNode.style.height = '100vh'
            el.parentNode.style.overflow = 'hidden'
            el.parentNode.style.position = 'relative'
            el.parentNode.style.padding = '20px'
            document.body.style.overflow = 'hidden'
        }
    }
}